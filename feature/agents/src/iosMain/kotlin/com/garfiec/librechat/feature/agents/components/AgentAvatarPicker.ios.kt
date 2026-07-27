package com.garfiec.librechat.feature.agents.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.garfiec.librechat.core.ui.platform.IosPickedImage
import com.garfiec.librechat.core.ui.platform.rememberIosImagePickerLauncher
import com.garfiec.librechat.feature.agents.resources.*
import com.garfiec.librechat.feature.agents.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun AgentAvatarPicker(
    avatarUrl: String?,
    agentName: String,
    onImageSelect: (Any) -> Unit,
    modifier: Modifier,
) {
    var selectedImage by remember { mutableStateOf<IosPickedImage?>(null) }
    val picker = rememberIosImagePickerLauncher {
        selectedImage = it
        onImageSelect(it)
    }
    val avatarCd = stringResource(Res.string.cd_agent_avatar_tap)

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable(onClick = picker::launch)
            .semantics { contentDescription = avatarCd },
        contentAlignment = Alignment.Center,
    ) {
        val preview = selectedImage?.bytes ?: avatarUrl
        if (preview != null) {
            AsyncImage(
                model = preview,
                contentDescription = stringResource(Res.string.cd_agent_avatar_name, agentName),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = agentName.take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
