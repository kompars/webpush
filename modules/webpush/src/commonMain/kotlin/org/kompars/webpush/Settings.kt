package org.kompars.webpush

import kotlin.time.*

public data class Settings(
    public val ttl: Duration? = null,
    public val topic: String? = null,
    public val urgency: Urgency? = null,
)

public enum class Urgency(internal val headerValue: String) {
    VeryLow("very-low"),
    Low("low"),
    Normal("normal"),
    High("high"),
}
