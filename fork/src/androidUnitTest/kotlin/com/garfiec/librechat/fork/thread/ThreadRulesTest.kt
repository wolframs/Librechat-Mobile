package com.garfiec.librechat.fork.thread

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadRulesTest {
    @Test fun replyLabelAppearsOnFirstReplyAndEitherIdentityChange() {
        assertTrue(showsModelLine("a", "one", null, null, false))
        assertFalse(showsModelLine("a", "one", "a", "one", true))
        assertTrue(showsModelLine("b", "one", "a", "one", true))
        assertTrue(showsModelLine("a", "two", "a", "one", true))
    }

    @Test fun dayLabelAppearsOnFirstValidDayAndDayChange() {
        val day = LocalDate(2026, 11, 13)
        assertTrue(showsDayLabel(day, null))
        assertFalse(showsDayLabel(day, day))
        assertFalse(showsDayLabel(null, day))
        assertTrue(showsDayLabel(LocalDate(2026, 11, 14), day))
    }

    @Test fun usableGroupsKeepUpstreamOrderBeforeSetupGroups() {
        assertEquals(listOf("usable-a", "usable-b", "key-a", "key-b"),
            usableGroupsFirst(listOf("key-a", "usable-a", "key-b", "usable-b")) { it.startsWith("key") })
        assertEquals(emptyList<String>(), usableGroupsFirst(emptyList<String>()) { true })
    }
}
