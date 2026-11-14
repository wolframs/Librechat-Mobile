package com.garfiec.librechat.feature.chat.fork

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.repository.PresetRepository
import com.garfiec.librechat.core.ui.components.AdaptiveAlertDialog
import com.garfiec.librechat.core.ui.components.AdaptiveOutlinedTextField
import com.garfiec.librechat.core.ui.components.AdaptiveSwitch
import com.garfiec.librechat.core.ui.components.AdaptiveTextButton
import com.garfiec.librechat.feature.chat.components.SavePresetDialog
import com.garfiec.librechat.feature.chat.model.PresetDisplayData
import com.garfiec.librechat.fork.icons.ForkIcons
import com.garfiec.librechat.fork.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt
import com.garfiec.librechat.feature.chat.resources.Res as ChatRes
import com.garfiec.librechat.feature.chat.resources.cancel as chatCancel
import com.garfiec.librechat.feature.chat.resources.delete as chatDelete
import com.garfiec.librechat.feature.chat.resources.retry as chatRetry
import com.garfiec.librechat.feature.chat.resources.save as chatSave

/** The header's add button: saves the chat's current model and settings as a new preset. */
@Composable
internal fun AddPresetButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(ForkIcons.Plus, stringResource(Res.string.composer_preset_add), Modifier.size(24.dp))
    }
}

/**
 * The model sheet's Presets page. A tap applies a preset, its model included; a sideways swipe
 * opens Edit and Delete on the side it uncovers. Every change goes through the view model, whose
 * reload after a server success is what this list and every other preset surface read, so the
 * sheet stays open and shows the result.
 */
@Composable
internal fun ComposerPresets(context: ComposerContext, saving: Boolean, onSavingChange: (Boolean) -> Unit, onApply: () -> Unit) {
    val state = context.state
    val vm = context.viewModel
    val presets = state.presets
    var open by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var editing by remember { mutableStateOf<PresetDisplayData?>(null) }
    var deleting by remember { mutableStateOf<PresetDisplayData?>(null) }
    val repository = koinInject<PresetRepository>()
    val scope = rememberCoroutineScope()
    // A rename keeps every stored field: the whole preset comes from the repository, only its title changes.
    val rename: (PresetDisplayData, String) -> Unit = { preset, name ->
        scope.launch {
            val stored = (repository.getAll() as? Result.Success)?.data?.firstOrNull { it.presetId == preset.presetId }
            stored?.let { vm.editPreset(it.copy(title = name)) }
        }
    }
    when {
        presets.isEmpty() && state.presetsLoadFailed -> ComposerGroup {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.composer_preset_failed), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                AdaptiveTextButton(onClick = vm::reloadPresetsIfFailed) { Text(stringResource(ChatRes.string.chatRetry)) }
            }
        }
        presets.isEmpty() -> Hint(stringResource(Res.string.composer_preset_none))
        else -> {
            ComposerGroup {
                presets.forEachIndexed { index, preset ->
                    val key = preset.presetId ?: "index:$index"
                    val side = open?.takeIf { it.first == key }?.second ?: 0
                    val edit = { open = null; editing = preset }
                    val delete = { open = null; deleting = preset }
                    SwipeRevealRow(
                        side = side,
                        onSide = { open = if (it == 0) null else key to it },
                        actions = { atStart ->
                            val editAction: @Composable () -> Unit = {
                                RevealAction(ForkIcons.Pencil, stringResource(Res.string.composer_preset_edit),
                                    MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, edit)
                            }
                            val deleteAction: @Composable () -> Unit = {
                                if (preset.presetId != null) {
                                    RevealAction(ForkIcons.Trash, stringResource(ChatRes.string.chatDelete),
                                        MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, delete)
                                }
                            }
                            // Delete sits at the outer edge on either side, Edit next to the row.
                            if (atStart) {
                                deleteAction()
                                editAction()
                            } else {
                                editAction()
                                deleteAction()
                            }
                        },
                        accessibilityActions = listOfNotNull(
                            CustomAccessibilityAction(stringResource(Res.string.composer_preset_edit)) { edit(); true },
                            preset.presetId?.let {
                                CustomAccessibilityAction(stringResource(ChatRes.string.chatDelete)) { delete(); true }
                            },
                        ),
                    ) {
                        val source = listOfNotNull(preset.model?.let { shortModelName(it) }, preset.endpointLabel).joinToString(" · ")
                        ChoiceRow(preset.title, source, checked = false, model = true) {
                            if (open != null) {
                                open = null
                            } else {
                                vm.loadPreset(preset)
                                onApply()
                            }
                        }
                    }
                    if (index < presets.lastIndex) ComposerDivider()
                }
            }
            Hint(stringResource(Res.string.composer_preset_hint))
        }
    }
    if (saving) {
        SavePresetDialog(
            currentEndpoint = state.endpointConfigs[state.selectedEndpoint]?.name ?: state.selectedEndpoint,
            currentModel = state.selectedModel,
            onSave = { name ->
                vm.savePreset(name.trim())
                onSavingChange(false)
            },
            onDismiss = { onSavingChange(false) },
        )
    }
    editing?.let { preset ->
        EditPresetDialog(preset, currentSettings(context), onDismiss = { editing = null }) { name, replace ->
            editing = null
            if (replace) vm.savePreset(name, replacing = preset.presetId) else rename(preset, name)
        }
    }
    deleting?.let { preset ->
        AdaptiveAlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(Res.string.composer_preset_delete_title)) },
            text = { Text(stringResource(Res.string.composer_preset_delete_text, preset.title)) },
            confirmButton = {
                AdaptiveTextButton(onClick = {
                    deleting = null
                    preset.presetId?.let(vm::deletePreset)
                }) { Text(stringResource(ChatRes.string.chatDelete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { AdaptiveTextButton(onClick = { deleting = null }) { Text(stringResource(ChatRes.string.chatCancel)) } },
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, Modifier.padding(horizontal = 32.dp), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun currentSettings(context: ComposerContext): String {
    val state = context.state
    val spec = state.modelSpecs?.list?.firstOrNull { it.name == state.selectedSpec }?.label
    return listOfNotNull(
        state.selectedModel?.let { shortModelName(it, spec) },
        state.endpointConfigs[state.selectedEndpoint]?.name ?: state.selectedEndpoint.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
}

@Composable
private fun EditPresetDialog(
    preset: PresetDisplayData,
    current: String,
    onDismiss: () -> Unit,
    onSave: (name: String, replaceSettings: Boolean) -> Unit,
) {
    var name by remember(preset.presetId) { mutableStateOf(preset.title) }
    var replace by remember(preset.presetId) { mutableStateOf(false) }
    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.composer_preset_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AdaptiveOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.composer_preset_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(stringResource(Res.string.composer_preset_replace), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(Res.string.composer_preset_replace_detail, current),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AdaptiveSwitch(checked = replace, onCheckedChange = { replace = it })
                }
            }
        },
        confirmButton = {
            AdaptiveTextButton(onClick = { onSave(name.trim(), replace) }, enabled = name.isNotBlank()) {
                Text(stringResource(ChatRes.string.chatSave))
            }
        },
        dismissButton = { AdaptiveTextButton(onClick = onDismiss) { Text(stringResource(ChatRes.string.chatCancel)) } },
    )
}

/**
 * A row that slides sideways to uncover [actions] on the side it leaves: a swipe to the left shows
 * them at the end, to the right at the start. [side] is -1, 0 or 1 and owned by the caller, so one
 * row is open at a time.
 */
@Composable
private fun SwipeRevealRow(
    side: Int,
    onSide: (Int) -> Unit,
    actions: @Composable (atStart: Boolean) -> Unit,
    accessibilityActions: List<CustomAccessibilityAction>,
    content: @Composable () -> Unit,
) {
    val reveal = with(LocalDensity.current) { RevealWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(side) { animate(offset, side * reveal) { value, _ -> offset = value } }
    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min).semantics { customActions = accessibilityActions }) {
        if (offset != 0f) {
            val start = offset > 0f
            Box(
                Modifier.align(if (start) Alignment.CenterStart else Alignment.CenterEnd)
                    .width(with(LocalDensity.current) { abs(offset).toDp() }).fillMaxHeight().clipToBounds(),
                contentAlignment = if (start) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(Modifier.wrapContentWidth(if (start) Alignment.Start else Alignment.End, unbounded = true).fillMaxHeight()) {
                    actions(start)
                }
            }
        }
        Box(
            Modifier.offset { IntOffset(offset.roundToInt(), 0) }.draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> offset = (offset + delta).coerceIn(-reveal, reveal) },
                onDragStopped = { velocity ->
                    val target = when {
                        offset < -reveal / 2 || (velocity < -FlingVelocity && offset < 0f) -> -1
                        offset > reveal / 2 || (velocity > FlingVelocity && offset > 0f) -> 1
                        else -> 0
                    }
                    if (target == side) animate(offset, side * reveal) { value, _ -> offset = value } else onSide(target)
                },
            ),
        ) { content() }
    }
}

@Composable
private fun RevealAction(icon: ImageVector, label: String, container: Color, contentColor: Color, onClick: () -> Unit) {
    Column(
        Modifier.requiredWidth(RevealWidth / 2).fillMaxHeight().background(container).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = contentColor)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

private val RevealWidth = 152.dp
private const val FlingVelocity = 800f
