package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.feature.chat.resources.Res
import com.garfiec.librechat.feature.chat.resources.cd_temporary_chat
import com.garfiec.librechat.feature.chat.resources.temporary_chat_enforced
import com.garfiec.librechat.fork.icons.ForkIcons
import com.garfiec.librechat.fork.resources.thread_temporary
import org.jetbrains.compose.resources.stringResource
import com.garfiec.librechat.fork.resources.Res as ForkRes

@Composable
internal fun ThreadTempChatToggle(
    isTemporary: Boolean,
    onToggle: () -> Unit,
    enforced: Boolean,
) {
    val isOn = isTemporary || enforced
    val description = stringResource(if (enforced) Res.string.temporary_chat_enforced else Res.string.cd_temporary_chat)
    val contentColor = if (isOn) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .then(if (isOn) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
            .toggleable(value = isOn, enabled = !enforced, role = Role.Switch, onValueChange = { onToggle() })
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ForkIcons.Ghost, contentDescription = null, modifier = Modifier.size(20.dp), tint = contentColor)
        Text(
            stringResource(ForkRes.string.thread_temporary),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
        )
    }
}
