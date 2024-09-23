package org.kompars.webpush.utils

import kotlin.io.encoding.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@OptIn(ExperimentalEncodingApi::class)
internal val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

@OptIn(ExperimentalEncodingApi::class)
internal object Base64Serializer : KSerializer<ByteArray> {
    override val descriptor = serialDescriptor<ByteArray>()

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(base64.encode(value))
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        return base64.decode(decoder.decodeString())
    }
}
