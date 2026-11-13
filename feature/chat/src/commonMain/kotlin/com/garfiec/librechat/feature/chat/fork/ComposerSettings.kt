package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garfiec.librechat.feature.chat.util.SettingChip
import com.garfiec.librechat.feature.chat.util.SettingChips
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.stringResource

internal fun visibleComposerChips(chips: SettingChips): List<SettingChip> = chips.chips.filter { chip ->
    when (chip) {
        is SettingChip.Mcp -> chip.server.status.needsAttention || chip.signInPending
        is SettingChip.Thinking -> chip.chip.pending != null
        is SettingChip.Tool -> false
    }
}

internal fun composerBadgeCount(shown: Boolean, tools: Set<String>, servers: Set<String>): Int =
    if (shown) tools.size + servers.size else 0

@Composable
internal fun ComposerToolsBadge(scope: BoxScope, shown: Boolean, tools: Set<String>, servers: Set<String>) {
    val count = composerBadgeCount(shown, tools, servers)
    if (count > 0) {
        with(scope) {
            Text(count.toString(), Modifier.align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape).padding(horizontal = 3.dp),
                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
internal fun conversationMediaLabel(): String = stringResource(Res.string.composer_media)
