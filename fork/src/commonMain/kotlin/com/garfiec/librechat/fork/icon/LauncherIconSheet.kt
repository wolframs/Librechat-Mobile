package com.garfiec.librechat.fork.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.ui.components.AdaptiveAlertDialog
import com.garfiec.librechat.core.ui.components.AdaptiveModalBottomSheet
import com.garfiec.librechat.core.ui.components.AdaptiveTextButton
import com.garfiec.librechat.fork.composer.ComposerDragHandle
import com.garfiec.librechat.fork.composer.ComposerSheetHeader
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun launcherIconLabel(): String = stringResource(Res.string.icon_title)

/** The name of the icon in force, for a settings row; null where the icon cannot be swapped. */
@Composable
fun launcherIconSummary(): String? = rememberLauncherIconSwitcher()?.current?.displayName

/** Owns the sheet, like the Text sheet, so no host needs navigation state for it. */
@Composable
fun rememberLauncherIconSheet(): () -> Unit {
    var open by remember { mutableStateOf(false) }
    val switcher = rememberLauncherIconSwitcher()
    if (open && switcher != null) LauncherIconSheet(switcher) { open = false }
    return { open = true }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherIconSheet(switcher: LauncherIconSwitcher, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(switcher.current) }
    var askToClose by remember { mutableStateOf(false) }
    AdaptiveModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { ComposerDragHandle() },
    ) {
        ComposerSheetHeader(launcherIconLabel(), onDismiss)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LauncherAlloy.entries.forEach { alloy ->
                AlloyTile(alloy, alloy == chosen, onClick = {
                    if (alloy != chosen) {
                        chosen = alloy
                        switcher.pick(alloy)
                        askToClose = true
                    }
                }, Modifier.weight(1f))
            }
        }
        Text(
            stringResource(Res.string.icon_note),
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (askToClose) {
        AdaptiveAlertDialog(
            onDismissRequest = { askToClose = false },
            confirmButton = { AdaptiveTextButton(onClick = switcher::closeNow) { Text(stringResource(Res.string.icon_close_now)) } },
            dismissButton = { AdaptiveTextButton(onClick = { askToClose = false }) { Text(stringResource(Res.string.icon_later)) } },
            title = { Text(stringResource(Res.string.icon_close_title)) },
            text = { Text(stringResource(Res.string.icon_close_text)) },
        )
    }
}

@Composable
private fun AlloyTile(alloy: LauncherAlloy, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(TileShape)
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainerHigh)
            .then(if (selected) Modifier.border(1.5.dp, colors.primary, TileShape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(painterResource(alloy.preview()), contentDescription = null, modifier = Modifier.size(64.dp))
        Text(alloy.displayName, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

private fun LauncherAlloy.preview(): DrawableResource = when (this) {
    LauncherAlloy.COLD_LAB -> Res.drawable.launcher_cold_lab
    LauncherAlloy.NIGHT_GLASS -> Res.drawable.launcher_night_glass
    LauncherAlloy.EMBER_ALCHEMY -> Res.drawable.launcher_ember_alchemy
}

private val TileShape = RoundedCornerShape(14.dp)
