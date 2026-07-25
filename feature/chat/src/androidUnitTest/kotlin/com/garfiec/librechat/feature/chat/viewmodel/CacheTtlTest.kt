package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.model.Message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CacheTtlTest {

    @Test
    fun armCyclesOneHourThenFiveMinutesThenOff() {
        val oneHour = null.nextCacheTtlArm()
        val fiveMinutes = oneHour.nextCacheTtlArm()
        val off = fiveMinutes.nextCacheTtlArm()

        assertEquals(CacheTtl.ONE_HOUR, oneHour)
        assertEquals(CacheTtl.FIVE_MINUTES, fiveMinutes)
        assertNull(off)
    }

    @Test
    fun remainingTimeUsesWebCompatibleFormatting() {
        assertEquals("60m", formatCacheTtlRemaining(3_599_000))
        assertEquals("11m", formatCacheTtlRemaining(601_000))
        assertEquals("10:00", formatCacheTtlRemaining(600_999))
        assertEquals("4:09", formatCacheTtlRemaining(249_999))
        assertEquals("0:00", formatCacheTtlRemaining(0))
    }

    @Test
    fun newestMessageSuppliesTimestampAndPersistedTtl() {
        val anchor = newestCacheTtlAnchor(
            listOf(
                message("old", "2026-07-25T10:00:00Z", cacheTtl = "5m"),
                message("new", "2026-07-25T10:01:00Z", cacheTtl = "1h"),
            ),
        )

        assertEquals("new", anchor?.messageId)
        assertEquals(CacheTtl.ONE_HOUR, anchor?.ttl)
    }

    @Test
    fun missingTimestampWinsForJustStreamedMessage() {
        val anchor = newestCacheTtlAnchor(
            listOf(
                message("persisted", "2026-07-25T10:00:00Z"),
                message("streaming", null),
            ),
        )

        assertEquals("streaming", anchor?.messageId)
        assertNull(anchor?.timestampMillis)
        assertEquals(CacheTtl.FIVE_MINUTES, anchor?.ttl)
    }

    @Test
    fun malformedTimestampIsIgnored() {
        val anchor = newestCacheTtlAnchor(
            listOf(
                message("broken", "yesterday-ish", cacheTtl = "1h"),
                message("valid", "2026-07-25T10:00:00Z"),
            ),
        )

        assertEquals("valid", anchor?.messageId)
    }

    @Test
    fun remainingTimeNeverGoesNegative() {
        assertEquals(
            0,
            cacheTtlRemainingMillis(
                anchorTimeMillis = 1_000,
                ttl = CacheTtl.FIVE_MINUTES,
                nowMillis = 1_000_000,
            ),
        )
    }

    private fun message(id: String, createdAt: String?, cacheTtl: String? = null) = Message(
        messageId = id,
        conversationId = "c1",
        createdAt = createdAt,
        cacheTTL = cacheTtl,
    )
}
