package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garfiec.librechat.core.ui.components.consumeUnhandledTouches
import com.garfiec.librechat.feature.chat.components.ChatOverflowItem
import com.garfiec.librechat.feature.chat.components.ChatOverflowMenu
import com.garfiec.librechat.feature.chat.components.ChatTopBarActions
import com.garfiec.librechat.feature.chat.components.InConvoSearchBar
import com.garfiec.librechat.feature.chat.components.TempChatToggle
import com.garfiec.librechat.feature.chat.resources.*
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState
import com.garfiec.librechat.fork.icons.ForkIcons
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ThreadTopBar(
    state: ChatUiState,
    actions: ChatTopBarActions,
    sections: List<List<ChatOverflowItem>>,
    onItem: (ChatOverflowItem) -> Unit,
    onOpenDrawer: (() -> Unit)?,
    showTempChatToggle: Boolean,
    openApps: Int,
    onRename: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier.background(MaterialTheme.colorScheme.background).consumeUnhandledTouches().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onOpenDrawer != null) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp)) {
                    Icon(ForkIcons.Menu, stringResource(Res.string.cd_open_drawer), Modifier.size(24.dp))
                }
            }
            val active = state.conversationId != null
            val titleModifier = if (active) {
                Modifier.clickable(onClickLabel = stringResource(Res.string.cd_edit_title), onClick = onRename)
            } else {
                Modifier
            }
            Text(
                if (active) state.conversationTitle.orEmpty() else "",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).then(titleModifier).heightIn(min = 48.dp).wrapContentHeight(),
            )
            if (showTempChatToggle) {
                TempChatToggle(
                    state.isTemporaryChat, actions.onToggleTemporaryChat, Modifier.size(48.dp),
                    enforced = state.temporaryChatEnforced,
                )
            }
            if (active) {
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(ForkIcons.EllipsisVertical, stringResource(Res.string.cd_more_options), Modifier.size(24.dp))
                    }
                    ChatOverflowMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        sections = sections,
                        isComparisonEnabled = state.comparisonState.isEnabled || state.comparisonState.isSettingUp,
                        contextGauge = state.contextGaugeDetails,
                        onItem = onItem,
                        shareLinkActive = state.shareLinkActive,
                        hasBookmark = state.hasBookmark,
                        canToggleComparison = state.canToggleComparison,
                        pullRequest = state.pullRequest.pullRequest,
                        openApps = openApps,
                    )
                }
            }
        }
        if (state.isSearchOpen) {
            InConvoSearchBar(
                query = state.searchQuery,
                onQueryChange = actions.onSearchQueryChange,
                currentMatchIndex = state.currentSearchMatchIndex,
                totalMatches = state.searchMatchIndices.size,
                onPreviousMatch = actions.onPreviousMatch,
                onNextMatch = actions.onNextMatch,
                onClose = actions.onCloseSearch,
            )
        }
    }
}
