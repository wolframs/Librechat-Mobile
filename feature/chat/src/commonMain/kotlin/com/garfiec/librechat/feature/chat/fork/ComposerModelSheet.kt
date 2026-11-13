package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.model.ModelRef
import com.garfiec.librechat.core.model.ParameterDefinition
import com.garfiec.librechat.core.ui.components.AdaptiveAssistChip
import com.garfiec.librechat.core.ui.components.AdaptiveModalBottomSheet
import com.garfiec.librechat.core.ui.components.AdaptiveTile
import com.garfiec.librechat.core.ui.components.EndpointParameterRegistry
import com.garfiec.librechat.core.ui.components.PlatformBackHandler
import com.garfiec.librechat.core.ui.components.clearFocusOnTap
import com.garfiec.librechat.feature.chat.components.ChatOptionsSheetController
import com.garfiec.librechat.feature.chat.components.ChatOverflowItem
import com.garfiec.librechat.feature.chat.resources.chat_agent_fallback_label
import com.garfiec.librechat.feature.chat.util.CompareQuickPick
import com.garfiec.librechat.feature.chat.util.compareQuickPicks
import com.garfiec.librechat.feature.chat.util.visibleSpecs
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState
import com.garfiec.librechat.feature.chat.viewmodel.ChatViewModel
import com.garfiec.librechat.fork.composer.ComposerDragHandle
import com.garfiec.librechat.fork.composer.ComposerSheetHeader
import com.garfiec.librechat.fork.icons.ForkIcons
import com.garfiec.librechat.fork.resources.*
import com.garfiec.librechat.fork.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.garfiec.librechat.feature.chat.resources.Res as ChatRes

internal data class ComposerContext(
    val state: ChatUiState,
    val viewModel: ChatViewModel,
    val options: ChatOptionsSheetController,
    val load: () -> Unit,
    val save: () -> Unit,
    val prompts: (() -> Unit)?,
    val cache: @Composable () -> Unit,
    val providerKeys: (String) -> Unit,
)

// Shares chat-owned actions with composer and both tools-sheet hosts.
@Suppress("CompositionLocalAllowlist")
internal val LocalComposer = staticCompositionLocalOf<ComposerContext?> { null }

// This scope bridges the existing screen-owned actions without changing platform signatures.
@Suppress("ViewModelForwarding")
@Composable
internal fun ComposerScope(
    state: ChatUiState,
    viewModel: ChatViewModel,
    options: ChatOptionsSheetController,
    load: () -> Unit,
    save: () -> Unit,
    prompts: (() -> Unit)?,
    cache: @Composable () -> Unit,
    providerKeys: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalComposer provides ComposerContext(state, viewModel, options, load, save, prompts, cache, providerKeys)) {
        content()
    }
}

internal fun conversationSections(sections: List<List<ChatOverflowItem>>): List<List<ChatOverflowItem>> =
    sections.map { section -> section.filterNot { it in relocatedActions } }.filter { it.isNotEmpty() }

private val relocatedActions =
    setOf(ChatOverflowItem.LOAD_PRESET, ChatOverflowItem.SAVE_PRESET, ChatOverflowItem.PROMPTS_LIBRARY, ChatOverflowItem.COMPARE)

internal fun shortModelName(name: String, specLabel: String? = null): String {
    if (!specLabel.isNullOrBlank()) return specLabel
    val id = name.substringAfter('/')
    if (!id.startsWith("claude-")) return id
    val parts = id.removePrefix("claude-").split('-')
    return parts.first().replaceFirstChar { it.uppercase() } +
        parts.drop(1).joinToString(".").let { if (it.isEmpty()) "" else " $it" }
}
internal const val ShowHeaderModel: Boolean = false
internal val HeaderModelText: String? = null

@Composable
internal fun messagePlaceholder(): String = stringResource(Res.string.composer_message)

private fun effortDefinition(state: ChatUiState): ParameterDefinition? {
    if (!state.parametersEnabled || state.comparisonState.isEnabled) return null
    val target = state.parameterTarget
    return EndpointParameterRegistry.getDefinitions(
        endpoint = target.settingsEndpoint,
        provider = target.settingsProvider,
        model = target.model,
        dropParams = target.dropParams(state.gates.dropParamsMap),
        extendedEffortSupported = state.extendedEffortSupported,
        thinkingDisplayUpdatesSupported = state.thinkingDisplayUpdatesSupported,
    ).firstOrNull { it.key in setOf("reasoning_effort", "thinkingLevel", "effort") && !it.options.isNullOrEmpty() }
}

internal fun modelSheetPicks(
    candidates: List<CompareQuickPick>, usage: List<ModelRef>, current: CompareQuickPick? = null,
): List<CompareQuickPick> {
    val starred = candidates.filter { it.starred }
    val ranked = if (starred.isNotEmpty()) {
        starred
    } else {
        usage.mapNotNull { ref ->
            candidates.firstOrNull { it.spec == ref.spec && it.endpoint == ref.endpoint && it.model == ref.model }
        }.distinct()
    }
    val unstarredCurrent = if (current != null && current !in starred) listOf(current) else emptyList()
    val picks = (unstarredCurrent + ranked).distinct().take(6)
    return if (current != null && current !in picks) picks.take(5) + current else picks
}

@Composable
internal fun ComposerModelChip(modifier: Modifier = Modifier) {
    val context = LocalComposer.current ?: return
    val state = context.state
    if (!state.modelPickerShown) return
    var open by remember { mutableStateOf(false) }
    val definition = effortDefinition(state)
    val modelLabel = com.garfiec.librechat.feature.chat.screen.rememberChatModelLabel(state.selectedEndpoint,
        state.selectedModel,
        state.agents,
        state.selectedSpec,
        state.modelSpecs)
    val effort = definition?.let { state.selection.sendParameters.getValueForKey(it.key) }.orEmpty()
    Box(modifier.height(48.dp), contentAlignment = Alignment.Center) {
        AdaptiveAssistChip(
            onClick = { open = true },
            modifier = Modifier.widthIn(max = 180.dp).height(34.dp),
            shape = CircleShape,
            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurface,
                trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = null,
            label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        shortModelName(state.selectedModel.orEmpty(),
                            state.modelSpecs?.list?.firstOrNull { it.name == state.selectedSpec }?.label
                            ?: modelLabel.displayModel.takeIf { state.selectedEndpoint == "agents" }),
                        Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (effort.isNotBlank()) {
                        Text(
                            effortLabel(effort),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            },
            trailingIcon = { Icon(ForkIcons.ChevronDown, null, Modifier.size(14.dp)) },
        )
    }
    if (open) ComposerModelSheet(context, definition) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposerModelSheet(context: ComposerContext, definition: ParameterDefinition?, dismiss: () -> Unit) {
    val state = context.state
    val vm = context.viewModel
    val store: SettingsDataStore = koinInject()
    val usage by remember(store) { store.topUsedModels(100) }.collectAsState(emptyList())
    LaunchedEffect(Unit) { vm.prepareModelSelector() }
    var page by remember { mutableStateOf("model") }
    val candidates = compareQuickPicks(
        query = "", availableModels = state.availableModels, endpointConfigs = state.endpointConfigs,
        agents = state.agents, favoriteModelKeys = state.favoriteModelKeys, favoriteAgentIds = state.favoriteAgentIds,
        favoriteSpecNames = state.favoriteSpecNames, agentCaption = stringResource(ChatRes.string.chat_agent_fallback_label),
        modelSelect = state.modelSelectEnabled,
        endpointKeyStates = state.endpointKeyStates, specs = state.modelSpecs.visibleSpecs(state.agents, state.agentsLoaded),
        modelSpecs = state.modelSpecs, serverUrl = state.serverUrl, limit = Int.MAX_VALUE,
    )
    val current = candidates.firstOrNull {
        it.spec == state.selectedSpec && it.endpoint == state.selectedEndpoint && it.model == state.selectedModel
    }
        ?: CompareQuickPick(state.selectedEndpoint, state.selectedModel.orEmpty(),
            state.modelSpecs?.list?.firstOrNull { it.name == state.selectedSpec }?.label ?: state.selectedModel.orEmpty(),
            state.endpointConfigs[state.selectedEndpoint]?.name ?: state.selectedEndpoint, false, spec = state.selectedSpec)
    val picks = modelSheetPicks(candidates, usage, current)
    val api = koinInject<com.garfiec.librechat.fork.market.MarketApi>()
    var markets by remember(state.serverUrl) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(state.serverUrl) { markets = api.endpoints() }
    val priceEndpoint = state.selectedAgentProvider.takeIf { state.selectedEndpoint == "agents" } ?: state.selectedEndpoint
    val priceModel = state.selection.selectedAgentModel.takeIf { state.selectedEndpoint == "agents" } ?: state.selectedModel
    AdaptiveModalBottomSheet(onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { ComposerDragHandle() },
        modifier = Modifier.clearFocusOnTap()) {
        PlatformBackHandler(enabled = page != "model") { page = "model" }
        if (page == "prices") {
            com.garfiec.librechat.fork.market.MarketPriceContent(state.serverUrl, priceModel.orEmpty(), onDismiss = { page = "model" })
            return@AdaptiveModalBottomSheet
        }
        if (page == "all") {
            ComposerAllModels(context, { page = "model" }, dismiss)
            return@AdaptiveModalBottomSheet
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ComposerSheetHeader(stringResource(when (page) {
                "effort" -> Res.string.composer_effort
                "presets" -> Res.string.composer_presets
                "parameters" -> Res.string.composer_parameters
                else -> Res.string.composer_model
            }), onNavigate = { if (page == "model") dismiss() else page = "model" }, subPage = page != "model")
            when (page) {
                "parameters" -> ComposerParameters(context)
                "effort" -> if (definition != null) {
                    ComposerGroup {
                        ChoiceRow(stringResource(Res.string.composer_default), stringResource(Res.string.composer_default_detail),
                            state.selection.sendParameters.getValueForKey(definition.key).isBlank(), !state.parametersSetBySpec) {
                            vm.updateModelParameters(state.selection.sendParameters.withUpdatedKey(definition.key, ""))
                            page = "model"
                        }
                        ComposerDivider()
                        definition.options.orEmpty().filter { it.isNotBlank() }.forEachIndexed { index, value ->
                            ChoiceRow(effortLabel(value),
                                null,
                                state.selection.sendParameters.getValueForKey(definition.key) == value,
                                !state.parametersSetBySpec) {
                                vm.updateModelParameters(state.selection.sendParameters.withUpdatedKey(definition.key, value))
                                page = "model"
                            }
                            if (index < definition.options.orEmpty().count { it.isNotBlank() } - 1) ComposerDivider()
                        }
                    }
                    Text(stringResource(Res.string.composer_effort_detail), Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "presets" -> ComposerGroup {
                    ComposerAction(stringResource(Res.string.composer_load), ForkIcons.BookOpen) { dismiss(); context.load() }
                    ComposerDivider()
                    ComposerAction(stringResource(Res.string.composer_save), ForkIcons.Bookmark) { dismiss(); context.save() }
                }
                else -> {
                    Text(stringResource(
                        if (candidates.any { it.starred }) Res.string.composer_favourites else Res.string.composer_most_used),
                        Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (picks.isNotEmpty()) {
                        ComposerGroup {
                            picks.forEachIndexed { index, pick ->
                                val selected = if (pick.spec != null) {
                                    state.selectedSpec == pick.spec
                                } else {
                                    state.selectedSpec == null &&
                                        state.selectedEndpoint == pick.endpoint && state.selectedModel == pick.model
                                }
                                ChoiceRow(shortModelName(pick.model,
                                    state.modelSpecs?.list?.firstOrNull { it.name == pick.spec }?.label
                                        ?: pick.name.takeIf { pick.endpoint == "agents" }),
                                    "${pick.model} · ${pick.source}", selected) {
                                    if (pick.spec != null) {
                                        state.modelSpecs?.list?.firstOrNull { it.name == pick.spec }?.let(vm::onSpecSelected)
                                    } else {
                                        vm.onModelSelected(pick.endpoint, pick.model)
                                    }
                                    dismiss()
                                }
                                if (index < picks.lastIndex) ComposerDivider()
                            }
                        }
                    }
                    ComposerGroup {
                        ComposerAction(stringResource(Res.string.composer_more), ForkIcons.Blocks) { page = "all" }
                    }
                    if (definition != null || state.parametersEnabled || state.presetsEnabled || priceEndpoint in markets) {
                        ComposerGroup {
                            if (definition != null) {
                                ComposerAction(
                                    stringResource(Res.string.composer_effort),
                                    ForkIcons.Gauge,
                                    effortLabel(state.selection.sendParameters.getValueForKey(definition.key)), trailingValue = true,
                                ) { page = "effort" }
                            }
                            if (state.parametersEnabled) {
                                if (definition != null) ComposerDivider()
                                ComposerAction(stringResource(Res.string.composer_parameters), ForkIcons.SlidersHorizontal) {
                                    page = "parameters"
                                }
                            }
                            if (state.presetsEnabled) {
                                if (state.parametersEnabled || definition != null) ComposerDivider()
                                ComposerAction(stringResource(Res.string.composer_presets), ForkIcons.BookmarkPlus) {
                                    page = "presets"
                                }
                            }
                            if (priceEndpoint in markets && !priceModel.isNullOrBlank()) {
                                if (definition != null || state.parametersEnabled || state.presetsEnabled) ComposerDivider()
                                ComposerAction(stringResource(Res.string.composer_prices), ForkIcons.DollarSign) { page = "prices" }
                            }
                        }
                    }
                    ComposerGroup {
                        if (state.multiConvoEnabled && state.modelPickerShown) {
                            ComposerAction(stringResource(Res.string.composer_compare), ForkIcons.Columns2) {
                                dismiss()
                                when { state.comparisonState.isEnabled -> vm.stopComparison()
                                    state.comparisonState.isSettingUp -> vm.cancelComparisonSetup()
                                    else -> vm.beginComparisonSetup() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun effortLabel(value: String): String = when (value) {
    "low" -> stringResource(Res.string.composer_low)
    "medium" -> stringResource(Res.string.composer_medium)
    "high" -> stringResource(Res.string.composer_high)
    "xhigh" -> stringResource(Res.string.composer_xhigh)
    "max" -> stringResource(Res.string.composer_max)
    "minimal" -> stringResource(Res.string.composer_minimal)
    "none" -> stringResource(Res.string.composer_none)
    "" -> stringResource(Res.string.composer_default)
    else -> value
}

@Composable
internal fun ComposerGroup(content: @Composable ColumnScope.() -> Unit) {
    AdaptiveTile(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Column(content = content) }
}

@Composable
internal fun ComposerDivider() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)))
}

@Composable
internal fun ComposerAction(
    label: String, icon: ImageVector, supporting: String? = null, trailingValue: Boolean = false, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(if (supporting.isNullOrBlank() || trailingValue) 56.dp else 64.dp)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            if (!trailingValue && !supporting.isNullOrBlank()) {
                Text(supporting, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailingValue && !supporting.isNullOrBlank()) {
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(ForkIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoiceRow(label: String, source: String?, checked: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(if (source.isNullOrBlank()) 56.dp else 64.dp).clickable(enabled = enabled,
        onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            if (!source.isNullOrBlank()) {
                Text(source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (checked) Icon(ForkIcons.Check, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun ComposerLeadingButtons(content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        content()
        ComposerModelChip(Modifier.weight(1f, fill = false))
        LocalComposer.current?.cache?.invoke()
    }
}

@Composable
internal fun composerCanFold(upstream: Boolean): Boolean = upstream && LocalComposer.current == null
