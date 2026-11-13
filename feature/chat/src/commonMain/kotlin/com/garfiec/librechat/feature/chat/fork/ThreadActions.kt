package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.ui.components.AdaptiveDropdownMenu
import com.garfiec.librechat.feature.chat.resources.Res
import com.garfiec.librechat.feature.chat.resources.cd_edit_message
import com.garfiec.librechat.feature.chat.resources.cd_fork_conversation
import com.garfiec.librechat.feature.chat.resources.cd_more_options
import com.garfiec.librechat.fork.thread.LocalQuietThread
import org.jetbrains.compose.resources.stringResource

/**
 * A reply's action row keeps what is used often; editing the reply and branching from it sit behind
 * a menu that names them. A user's own row is short and stays whole. [row] draws upstream's buttons
 * with the edit and branch actions it is handed.
 */
@Composable
internal fun ThreadActions(
    isUser: Boolean,
    onEdit: (() -> Unit)?,
    onFork: (() -> Unit)?,
    modifier: Modifier = Modifier,
    row: @Composable (onEdit: (() -> Unit)?, onFork: (() -> Unit)?, modifier: Modifier) -> Unit,
) {
    val behindMenu = !isUser && LocalQuietThread.current && (onEdit != null || onFork != null)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        row(onEdit.takeUnless { behindMenu }, onFork.takeUnless { behindMenu }, Modifier.weight(1f, fill = false))
        if (behindMenu) ReplyMenu(onEdit, onFork)
    }
}

@Composable
private fun ReplyMenu(onEdit: (() -> Unit)?, onFork: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.MoreHoriz, stringResource(Res.string.cd_more_options),
                Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AdaptiveDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            onEdit?.let { edit ->
                NamedAction(Icons.Default.Edit, stringResource(Res.string.cd_edit_message)) {
                    open = false
                    edit()
                }
            }
            onFork?.let { fork ->
                NamedAction(Icons.AutoMirrored.Filled.CallSplit, stringResource(Res.string.cd_fork_conversation)) {
                    open = false
                    fork()
                }
            }
        }
    }
}

@Composable
private fun NamedAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Icon(icon, contentDescription = null) }, onClick = onClick)
}
