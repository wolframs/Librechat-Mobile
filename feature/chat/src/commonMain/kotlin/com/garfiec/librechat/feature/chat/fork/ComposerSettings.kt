package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
            Box(
                // Hangs off the icon's top corner, as a badge does, so the plus itself stays whole.
                Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = (-2).dp)
                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(count.toString(), fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.surface)
            }
        }
    }
}

@Composable
internal fun conversationMediaLabel(): String = stringResource(Res.string.composer_media)
