package org.kompars.webpush

import kotlin.io.encoding.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.kompars.webpush.utils.*

private val jsonFormat = Json {
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalEncodingApi::class)
@Serializable
public class Subscription(
    public val endpoint: String,
    public val expirationTime: Long? = null,
    public val keys: Keys,
) {
    public constructor(endpoint: String, p256dh: ByteArray, auth: ByteArray) : this(endpoint, null, Keys(p256dh, auth))

    @Serializable
    public class Keys(
        @Serializable(with = Base64Serializer::class)
        public val p256dh: ByteArray,
        @Serializable(with = Base64Serializer::class)
        public val auth: ByteArray,
    )

    public companion object {
        public fun fromJson(json: String): Subscription {
            return jsonFormat.decodeFromString(json)
        }
    }

    public fun toJson(): String {
        return jsonFormat.encodeToString(this)
    }

    override fun toString(): String {
        return "Subscription(endpoint=$endpoint, p256dh=${base64.encode(keys.p256dh)}, auth=${base64.encode(keys.auth)})"
    }
}
