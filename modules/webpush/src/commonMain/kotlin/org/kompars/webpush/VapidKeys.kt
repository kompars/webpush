package org.kompars.webpush

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.serialization.asn1.*
import dev.whyoleg.cryptography.serialization.asn1.modules.*
import kotlin.io.encoding.*
import kotlin.lazy
import kotlinx.serialization.*
import org.kompars.webpush.utils.*

@OptIn(ExperimentalEncodingApi::class)
public class VapidKeys internal constructor(
    internal val publicKey: ECDSA.PublicKey,
    internal val privateKey: ECDSA.PrivateKey,
) {
    public companion object {
        public fun generate(): VapidKeys {
            val keyPair = CryptographyProvider.Default
                .get(ECDSA)
                .keyPairGenerator(EC.Curve.P256)
                .generateKeyBlocking()

            return VapidKeys(keyPair.publicKey, keyPair.privateKey)
        }

        public fun decode(publicKey: String, privateKey: String): VapidKeys {
            val decodedPublicKey = CryptographyProvider.Default
                .get(ECDSA)
                .publicKeyDecoder(EC.Curve.P256)
                .decodeFromByteArrayBlocking(EC.PublicKey.Format.RAW, base64.decode(publicKey))

            val derPrivateKey = Der.encodeToByteArray(EcPrivateKey(1, base64.decode(privateKey), EcParameters(ObjectIdentifier.secp256r1)))

            val decodedPrivateKey = CryptographyProvider.Default
                .get(ECDSA)
                .privateKeyDecoder(EC.Curve.P256)
                .decodeFromByteArrayBlocking(EC.PrivateKey.Format.DER.SEC1, derPrivateKey)

            return VapidKeys(decodedPublicKey, decodedPrivateKey)
        }
    }

    public val encodedPublicKey: String by lazy {
        base64.encode(publicKey.encodeToByteArrayBlocking(EC.PublicKey.Format.RAW))
    }

    public val encodedPrivateKey: String by lazy {
        privateKey
            .encodeToByteArrayBlocking(EC.PrivateKey.Format.DER.SEC1)
            .let { Der.decodeFromByteArray<EcPrivateKey>(it) }
            .privateKey
            .let { base64.encode(it) }
    }

    override fun toString(): String {
        return "VapidKeys(publicKey=$encodedPublicKey, privateKey=$encodedPrivateKey)"
    }
}
