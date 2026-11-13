package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.common.ToolConstants
import com.garfiec.librechat.core.ui.components.ModelParameterContent
import com.garfiec.librechat.fork.composer.ComposerSheetHeader
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ComposerAllModels(context: ComposerContext, back: () -> Unit, dismiss: () -> Unit) {
    val s = context.state
    val vm = context.viewModel
    ComposerAllModelContent(
        endpointConfigs = s.endpointConfigs, availableModels = s.availableModels, agents = s.agents,
        selectedEndpoint = s.selectedEndpoint, selectedModel = s.selectedModel, modelSelect = s.modelSelectEnabled,
        onModelSelect = { endpoint, model -> vm.onModelSelected(endpoint, model); dismiss() },
        onSetApiKey = { dismiss(); context.providerKeys(it) },
        modelSpecs = s.modelSpecs, selectedSpec = s.selectedSpec,
        onSpecSelect = { vm.onSpecSelected(it); dismiss() }, agentsLoaded = s.agentsLoaded,
        favoriteAgentIds = s.favoriteAgentIds, favoriteModelKeys = s.favoriteModelKeys,
        favoriteSpecNames = s.favoriteSpecNames, onToggleAgentFavorite = vm::toggleAgentFavorite,
        onToggleModelFavorite = vm::toggleModelFavorite, onToggleSpecFavorite = vm::toggleSpecFavorite,
        starredDisplay = s.starredModelsDisplay, endpointKeyStates = s.endpointKeyStates,
        showTitle = false, modifier = Modifier.fillMaxWidth().height(560.dp),
        header = { ComposerSheetHeader(stringResource(Res.string.composer_more), onNavigate = back, subPage = true) },
    )
}

@Composable
internal fun ComposerParameters(context: ComposerContext) {
    val s = context.state
    val target = s.parameterTarget
    ModelParameterContent(
        parameters = s.selection.sendParameters, onParametersChange = context.viewModel::updateModelParameters,
        selectedEndpoint = target.settingsEndpoint, selectedProvider = target.settingsProvider,
        selectedModel = target.model, dropParams = target.dropParams(s.gates.dropParamsMap),
        extendedEffortSupported = s.extendedEffortSupported,
        thinkingDisplayUpdatesSupported = s.thinkingDisplayUpdatesSupported,
        onSaveAsPreset = context.save, showHeader = false, applyVerticalScroll = false,
        readOnlyNote = if (s.parametersSetBySpec) stringResource(Res.string.composer_locked) else null,
        hiddenKeys = setOf(ToolConstants.WEB_SEARCH, ToolConstants.URL_CONTEXT),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
