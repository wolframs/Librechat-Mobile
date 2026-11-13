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
        assertEquals(listOf(tool, idle), foldedTools(chips.chips))
        assertEquals(listOf(signIn, failed, pending, locked, off), visibleComposerChips(chips))
        assertEquals(listOf(tool), visibleComposerChips(SettingChips(listOf(tool))))
    }
}
