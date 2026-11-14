package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.common.EndpointConstants
import com.garfiec.librechat.core.common.ToolConstants
import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.repository.PresetRepository
import com.garfiec.librechat.core.model.Preset
import com.garfiec.librechat.core.model.endpoint.endpointDisplayName
import com.garfiec.librechat.core.ui.components.AdaptiveButton
import com.garfiec.librechat.core.ui.components.AdaptiveCircularProgressIndicator
import com.garfiec.librechat.core.ui.components.AdaptiveExposedDropdownMenu
import com.garfiec.librechat.core.ui.components.AdaptiveExposedDropdownMenuBox
import com.garfiec.librechat.core.ui.components.AdaptiveOutlinedButton
import com.garfiec.librechat.core.ui.components.AdaptiveOutlinedTextField
import com.garfiec.librechat.core.ui.components.ModelParameterContent
import com.garfiec.librechat.feature.chat.util.parameterTarget
import com.garfiec.librechat.fork.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The preset editor: name, endpoint, model and the parameter controls of that parameter set, seeded
 * from the stored preset. Save stores the edited copy as a new preset and deletes the old one once
 * the copy exists, because the server's update only sets the fields a request names and would keep
 * a value the editor reset; the list reloads, and [onDone] returns to it.
 */
@Composable
internal fun ComposerPresetEditor(context: ComposerContext, presetId: String, onDone: () -> Unit) {
    val repository = koinInject<PresetRepository>()
    var stored by remember(presetId) { mutableStateOf<Preset?>(null) }
    var failed by remember(presetId) { mutableStateOf(false) }
    LaunchedEffect(presetId) {
        stored = (repository.getAll() as? Result.Success)?.data?.firstOrNull { it.presetId == presetId }
        failed = stored == null
    }
    val preset = stored
    when {
        preset != null -> PresetForm(context, preset, repository, onDone)
        failed -> Text(stringResource(Res.string.composer_preset_failed), Modifier.padding(horizontal = 16.dp))
        else -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { AdaptiveCircularProgressIndicator() }
    }
}

@Composable
private fun PresetForm(context: ComposerContext, preset: Preset, repository: PresetRepository, onDone: () -> Unit) {
    val state = context.state
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(preset.title.orEmpty()) }
    var endpoint by remember { mutableStateOf(preset.endpoint ?: state.selectedEndpoint) }
    var model by remember { mutableStateOf(preset.model) }
    var params by remember { mutableStateOf(preset.editorParameters()) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val target = parameterTarget(endpoint, model, state.agents, state.endpointConfigs)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdaptiveOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.composer_preset_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (endpoint == EndpointConstants.AGENTS) {
                // An agent preset names its agent, not a model: the pair stays as stored.
                Text(listOfNotNull(state.agents.firstOrNull { it.id == model }?.name ?: model,
                    endpointDisplayName(endpoint, state.endpointConfigs[endpoint])).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                EndpointField(context, endpoint) { picked ->
                    if (picked != endpoint) {
                        endpoint = picked
                        model = state.availableModels[picked]?.firstOrNull()
                    }
                }
                ModelField(state.availableModels[endpoint].orEmpty(), model) { model = it }
            }
            AdaptiveOutlinedButton(onClick = {
                endpoint = state.selectedEndpoint
                model = state.selectedModel
                params = state.selection.sendParameters
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.composer_preset_from_chat)) }
        }
        ModelParameterContent(
            parameters = params, onParametersChange = { params = it },
            selectedEndpoint = target.settingsEndpoint, selectedProvider = target.settingsProvider, selectedModel = target.model,
            dropParams = target.dropParams(state.gates.dropParamsMap),
            extendedEffortSupported = state.extendedEffortSupported,
            thinkingDisplayUpdatesSupported = state.thinkingDisplayUpdatesSupported,
            showHeader = false, showSaveAsPreset = false, applyVerticalScroll = false,
            hiddenKeys = setOf(ToolConstants.WEB_SEARCH, ToolConstants.URL_CONTEXT),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (saveFailed) {
                Text(stringResource(Res.string.composer_preset_save_failed), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
            AdaptiveButton(
                onClick = {
                    saving = true
                    saveFailed = false
                    scope.launch {
                        val copy = preset.edited(name.trim(), endpoint, model, params, target)
                        if (repository.create(copy) is Result.Success) {
                            preset.presetId?.let { repository.delete(it) }
                            context.viewModel.reloadPresets()
                            onDone()
                        } else {
                            saving = false
                            saveFailed = true
                        }
                    }
                },
                enabled = name.isNotBlank() && !saving,
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.composer_preset_save)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndpointField(context: ComposerContext, endpoint: String, onPick: (String) -> Unit) {
    val state = context.state
    var expanded by remember { mutableStateOf(false) }
    val options = state.availableModels.filterValues { it.isNotEmpty() }.keys
        .filter { it != EndpointConstants.AGENTS }
        .sortedBy { endpointDisplayName(it, state.endpointConfigs[it]).lowercase() }
    AdaptiveExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        AdaptiveOutlinedTextField(
            value = endpointDisplayName(endpoint, state.endpointConfigs[endpoint]),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.composer_preset_endpoint)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        AdaptiveExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { key ->
                DropdownMenuItem(text = { Text(endpointDisplayName(key, state.endpointConfigs[key])) }, onClick = {
                    expanded = false
                    onPick(key)
                })
            }
        }
    }
}

/** The models of the chosen endpoint; typing filters them, so a list of hundreds stays usable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelField(models: List<String>, model: String?, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(model) { mutableStateOf(model.orEmpty()) }
    val shown = if (query == model) models else models.filter { it.contains(query, ignoreCase = true) }
    AdaptiveExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        AdaptiveOutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            singleLine = true,
            label = { Text(stringResource(Res.string.composer_preset_model)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        AdaptiveExposedDropdownMenu(expanded = expanded && shown.isNotEmpty(), onDismissRequest = {
            expanded = false
            query = model.orEmpty()
        }) {
            shown.take(MAX_MODELS_SHOWN).forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    expanded = false
                    query = option
                    onPick(option)
                })
            }
        }
    }
}

private const val MAX_MODELS_SHOWN = 60
