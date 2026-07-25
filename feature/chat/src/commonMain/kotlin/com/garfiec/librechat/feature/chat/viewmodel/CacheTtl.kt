package com.garfiec.librechat.feature.chat.viewmodel

import androidx.compose.runtime.Immutable
import com.garfiec.librechat.core.model.Message
import kotlin.time.Instant

/** Anthropic's supported ephemeral prompt-cache lifetimes. */
enum class CacheTtl(
    val wireValue: String,
    val durationMillis: Long,
) {
    FIVE_MINUTES("5m", 5 * 60 * 1_000L),
    ONE_HOUR("1h", 60 * 60 * 1_000L),
    ;

    companion object {
        fun fromWire(value: String?): CacheTtl =
            if (value == ONE_HOUR.wireValue) ONE_HOUR else FIVE_MINUTES
    }
}

/** Cycles the one-shot composer arm in the same order as LibreChat web. */
fun CacheTtl?.nextCacheTtlArm(): CacheTtl? = when (this) {
    null -> CacheTtl.ONE_HOUR
    CacheTtl.ONE_HOUR -> CacheTtl.FIVE_MINUTES
    CacheTtl.FIVE_MINUTES -> null
}

@Immutable
data class CacheTtlAnchor(
    val messageId: String,
    val timestampMillis: Long?,
    val ttl: CacheTtl,
)

/**
 * Finds the newest message by `createdAt ?? updatedAt`, matching the web overlay.
 * Missing timestamps sort newest so a just-streamed message can immediately arm the timer;
 * malformed timestamps are ignored.
 */
fun newestCacheTtlAnchor(messages: List<Message>): CacheTtlAnchor? {
    var newest: CacheTtlAnchor? = null
    var newestSortTime = Long.MIN_VALUE

    messages.forEach { message ->
        val timestamp = message.createdAt ?: message.updatedAt
        val parsed = timestamp?.let {
            try {
                Instant.parse(it).toEpochMilliseconds()
            } catch (_: IllegalArgumentException) {
                return@forEach
            }
        }
        val sortTime = parsed ?: Long.MAX_VALUE
        if (sortTime >= newestSortTime) {
            newestSortTime = sortTime
            newest = CacheTtlAnchor(
                messageId = message.messageId,
                timestampMillis = parsed,
                ttl = CacheTtl.fromWire(message.cacheTTL),
            )
        }
    }
    return newest
}

fun cacheTtlRemainingMillis(anchorTimeMillis: Long, ttl: CacheTtl, nowMillis: Long): Long =
    (anchorTimeMillis + ttl.durationMillis - nowMillis).coerceAtLeast(0L)

/**
 * Web-compatible timer formatting: beyond ten minutes show rounded-up whole minutes,
 * otherwise show `m:ss`.
 */
fun formatCacheTtlRemaining(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis.coerceAtLeast(0L) / 1_000L)
    if (totalSeconds > 600L) {
        return "${(totalSeconds + 59L) / 60L}m"
    }
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
