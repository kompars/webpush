package org.kompars.webpush

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.random.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlin.io.encoding.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.serialization.json.*
import org.kompars.webpush.utils.*

@OptIn(ExperimentalEncodingApi::class)
public class WebPush(
    private val subject: String,
    private val vapidKeys: VapidKeys,
    private val httpClient: HttpClient = HttpClient(),
) {
    public val applicationServerKey: ByteArray = vapidKeys.publicKey.encodeToByteArrayBlocking(EC.PublicKey.Format.RAW)

    private companion object {
        private val WEBPUSH_INFO: ByteArray = "WebPush: info\u0000".toByteArray()
        private val KEY_INFO: ByteArray = "Content-Encoding: aes128gcm\u0000".toByteArray()
        private val NONCE_INFO: ByteArray = "Content-Encoding: nonce\u0000".toByteArray()

        private val DEFAULT_TTL: Long = 28.days.inWholeSeconds
        private val TOKEN_TTL: Long = 12.hours.inWholeSeconds
    }

    private val encodedApplicationServerKey: String = base64.encode(applicationServerKey)

    public suspend fun send(subscription: Subscription, payload: ByteArray, settings: Settings? = null): SubscriptionState {
        val token = createToken(subscription)
        val body = encryptBody(payload, subscription.keys.p256dh, subscription.keys.auth)

        val response = httpClient.post(subscription.endpoint) {
            header("Authorization", "vapid t=$token, k=$encodedApplicationServerKey")
            header("Content-Encoding", "aes128gcm")
            header("Content-Type", "application/octet-stream")
            header("TTL", settings?.ttl?.inWholeSeconds ?: DEFAULT_TTL)
            header("Urgency", settings?.urgency?.headerValue)
            header("Topic", settings?.topic)
            setBody(body)
        }

        val responseText = response.bodyAsText().take(200)

        return when (response.status.value) {
            200, 201, 202 -> SubscriptionState.ACTIVE
            404, 410 -> SubscriptionState.EXPIRED
            401, 403 -> throw WebPushException("Authentication failed: $responseText")
            502, 503 -> throw WebPushException("Service unavailable: $responseText")
            else -> throw WebPushException("Unexpected response: $responseText")
        }
    }

    @OptIn(DelicateCryptographyApi::class)
    private fun encryptBody(payload: ByteArray, p256dh: ByteArray, auth: ByteArray): ByteArray {
        val userPublicKey = CryptographyProvider.Default
            .get(ECDH)
            .publicKeyDecoder(EC.Curve.P256)
            .decodeFromByteArrayBlocking(EC.PublicKey.Format.RAW, p256dh)

        val auxKeyPair = CryptographyProvider.Default
            .get(ECDH)
            .keyPairGenerator(EC.Curve.P256)
            .generateKeyBlocking()

        val auxPublicKey = auxKeyPair.publicKey
            .encodeToByteArrayBlocking(EC.PublicKey.Format.RAW)

        val secret = auxKeyPair.privateKey
            .sharedSecretGenerator()
            .generateSharedSecretBlocking(userPublicKey)
            .toByteArray()

        val salt = CryptographyRandom.nextBytes(16)
        val secretInfo = concatBytes(WEBPUSH_INFO, p256dh, auxPublicKey)
        val derivedSecret = hkdfSha256(secret, auth, secretInfo, 32)
        val derivedKey = hkdfSha256(derivedSecret, salt, KEY_INFO, 16)
        val derivedNonce = hkdfSha256(derivedSecret, salt, NONCE_INFO, 12)

        val encryptedPayload = CryptographyProvider.Default
            .get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArrayBlocking(AES.Key.Format.RAW, derivedKey)
            .cipher()
            .encryptWithIvBlocking(derivedNonce, payload + byteArrayOf(2), null)

        return concatBytes(
            salt,
            byteArrayOf(0, 0, 16, 0),
            byteArrayOf(auxPublicKey.size.toByte()),
            auxPublicKey,
            encryptedPayload,
        )
    }

    private fun hkdfSha256(input: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        return hmacSha256(hmacSha256(salt, input), info + 0x01.toByte()).copyOfRange(0, length)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        return CryptographyProvider.Default
            .get(HMAC)
            .keyDecoder(SHA256)
            .decodeFromByteArrayBlocking(HMAC.Key.Format.RAW, key)
            .signatureGenerator()
            .generateSignatureBlocking(data)
    }

    private suspend fun createToken(subscription: Subscription): String {
        val audience = Url(subscription.endpoint).protocolWithAuthority
        val expiresAt = Clock.System.now().toEpochMilliseconds() / 1000 + TOKEN_TTL

        val payload = """{"sub":"$subject","aud":"$audience","exp":$expiresAt}"""

        val encodedHeader = base64.encode("""{"alg":"ES256","typ":"JWT"}""".encodeToByteArray())
        val encodedPayload = base64.encode(payload.encodeToByteArray())
        val encodedToken = "$encodedHeader.$encodedPayload"

        val signature = vapidKeys.privateKey
            .signatureGenerator(SHA256, ECDSA.SignatureFormat.DER)
            .generateSignature(encodedToken.encodeToByteArray())

        val encodedSignature = base64.encode(convertDerToJose(signature))

        return "$encodedToken.$encodedSignature"
    }

    private fun concatBytes(vararg arrays: ByteArray): ByteArray {
        val result = ByteArray(arrays.sumOf { it.size })
        var position = 0

        for (array in arrays) {
            array.copyInto(result, position)
            position += array.size
        }

        return result
    }
}

public fun WebPush(subject: String, publicKey: String, privateKey: String, httpClient: HttpClient = HttpClient()): WebPush {
    return WebPush(subject, VapidKeys.decode(publicKey, privateKey), httpClient)
}

public suspend fun WebPush.send(subscription: Subscription, payload: String, settings: Settings? = null): SubscriptionState {
    return send(subscription, payload.encodeToByteArray(), settings)
}

public suspend inline fun <reified T> WebPush.send(subscription: Subscription, payload: T, settings: Settings? = null): SubscriptionState {
    return send(subscription, Json.encodeToString(payload), settings)
}
