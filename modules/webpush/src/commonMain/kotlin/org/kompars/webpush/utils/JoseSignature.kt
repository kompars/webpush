package org.kompars.webpush.utils

import kotlin.math.*

internal fun convertDerToJose(der: ByteArray): ByteArray {
    val numberSize = 32
    var offset = 3
    val jose = ByteArray(numberSize * 2)

    if (der[1] == 0x81.toByte()) {
        offset++
    }

    val rLength = der[offset++].toInt()
    val rPadding = numberSize - rLength

    der.copyInto(
        destination = jose,
        destinationOffset = max(rPadding, 0),
        startIndex = offset + max(-rPadding, 0),
        endIndex = offset + max(-rPadding, 0) + (rLength - max(-rPadding, 0))
    )

    offset += rLength + 1

    val sLength = der[offset++].toInt()
    val sPadding = numberSize - sLength

    der.copyInto(
        destination = jose,
        destinationOffset = numberSize + max(sPadding, 0),
        startIndex = offset + max(-sPadding, 0),
        endIndex = offset + max(-sPadding, 0) + (sLength - max(-sPadding, 0))
    )

    return jose
}
