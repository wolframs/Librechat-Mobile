package com.garfiec.librechat.core.ui.media

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.ui.components.AdaptiveCircularProgressIndicator

/**
 * The standard save/share toolbar for [ZoomableMediaPager], shared by every surface that hosts the
 * viewer (chat, files) so the button set, icons, white tint, and the save null-guard live in one
 * place instead of being re-declared per surface.
 *
 * [onSave] is null on platforms/surfaces without a save (e.g. iOS for a plain image URL), in which
 * case the save button is omitted; [onOpenIn] is null unless the item's original file can be handed
 * to another app (a generated file, whose bytes the caller downloads). Content descriptions are
 * passed in (resolved once by the caller, above the pager) so paging between items doesn't
 * re-resolve string resources on every swipe. [busy] is the action whose download is in flight:
 * its button shows a spinner and every button is held until it lands, as in [FileActionsSheet].
 *
 * No `modifier` param: this emits sibling buttons directly into the caller-owned [RowScope], so
 * there is no single root node to apply one to.
 */
@Suppress("ModifierMissing")
@Composable
fun RowScope.MediaActionBar(
    item: MediaItem,
    onSave: ((url: String) -> Unit)?,
    onShare: (url: String) -> Unit,
    saveContentDescription: String,
    shareContentDescription: String,
    onOpenIn: ((url: String) -> Unit)? = null,
    openInContentDescription: String = "",
    busy: FileAction? = null,
) {
    if (onOpenIn != null) {
        MediaActionButton(Icons.AutoMirrored.Filled.OpenInNew, openInContentDescription, busy, FileAction.OPEN_IN) {
            onOpenIn(item.url)
        }
    }
    if (onSave != null) {
        MediaActionButton(Icons.Outlined.SaveAlt, saveContentDescription, busy, FileAction.SAVE) { onSave(item.url) }
    }
    MediaActionButton(Icons.Default.Share, shareContentDescription, busy, FileAction.SHARE) { onShare(item.url) }
}

@Composable
private fun MediaActionButton(
    icon: ImageVector,
    contentDescription: String,
    busy: FileAction?,
    action: FileAction,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = busy == null) {
        if (busy == action) {
            AdaptiveCircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
        }
    }
}
