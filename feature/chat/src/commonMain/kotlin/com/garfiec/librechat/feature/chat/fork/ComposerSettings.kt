package com.garfiec.librechat.feature.chat.fork

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.garfiec.librechat.core.model.mcp.McpDisplayStatus
import com.garfiec.librechat.core.ui.components.AdaptiveAssistChip
import com.garfiec.librechat.feature.chat.util.SettingChip
import com.garfiec.librechat.feature.chat.util.SettingChipState
import com.garfiec.librechat.feature.chat.util.SettingChips
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.stringResource

internal fun foldedTools(chips: List<SettingChip>): List<SettingChip> = chips.filter { chip ->
    chip.state == SettingChipState.ON && when (chip) {
        is SettingChip.Tool -> true
        is SettingChip.Mcp -> !chip.server.status.needsAttention && !chip.signInPending &&
            chip.status != McpDisplayStatus.CONNECTING
        else -> false
    }
}

internal fun visibleComposerChips(chips: SettingChips): List<SettingChip> {
    val folded = foldedTools(chips.chips)
    return if (folded.size < 2) chips.chips else chips.chips - folded.toSet()
}

@Composable
internal fun ComposerToolsSummary(chips: SettingChips) {
    val context = LocalComposer.current ?: return
    val count = foldedTools(chips.chips).size
    if (count >= 2) {
        AdaptiveAssistChip(onClick = { context.options.open(com.garfiec.librechat.feature.chat.components.ChatOptionsPage.Options) },
        label = { Text(stringResource(Res.string.composer_tools_count, count)) })
    }
}

internal const val ShowToolsDot: Boolean = false

@Composable
internal fun conversationSearchLabel(): String = stringResource(Res.string.composer_search)

@Composable
internal fun conversationMediaLabel(): String = stringResource(Res.string.composer_media)
