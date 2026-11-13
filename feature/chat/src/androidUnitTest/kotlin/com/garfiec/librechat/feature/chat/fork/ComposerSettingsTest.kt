package com.garfiec.librechat.feature.chat.fork

import com.garfiec.librechat.core.model.mcp.McpDisplayStatus
import com.garfiec.librechat.feature.chat.model.McpServerDisplayData
import com.garfiec.librechat.feature.chat.util.SettingChip
import com.garfiec.librechat.feature.chat.util.SettingChipState
import com.garfiec.librechat.feature.chat.util.SettingChips
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposerSettingsTest {
    private val tool = SettingChip.Tool("web_search", SettingChipState.ON, false)
    private fun server(status: McpDisplayStatus) = SettingChip.Mcp(
        McpServerDisplayData("server", null, null, status), SettingChipState.ON)

    @Test fun foldsOnlyPlainOnStatesAndKeepsAttentionAndLocks() {
        val idle = server(McpDisplayStatus.DISCONNECTED)
        val signIn = server(McpDisplayStatus.NEEDS_SIGN_IN)
        val failed = server(McpDisplayStatus.FAILED)
        val pending = server(McpDisplayStatus.CONNECTING)
        val locked = tool.copy(state = SettingChipState.LOCKED)
        val off = tool.copy(state = SettingChipState.PINNED_OFF)
        val chips = SettingChips(listOf(tool, idle, signIn, failed, pending, locked, off))
        assertEquals(listOf(signIn, failed), visibleComposerChips(chips))
        assertEquals(emptyList(), visibleComposerChips(SettingChips(listOf(tool))))
    }

    @Test fun badgeCountsToolsAndConnectorsOnlyWhenShown() {
        assertEquals(3, composerBadgeCount(true, setOf("web_search", "skills"), setOf("server")))
        assertEquals(0, composerBadgeCount(true, emptySet(), emptySet()))
        assertEquals(0, composerBadgeCount(false, setOf("skills"), setOf("server")))
    }
}
