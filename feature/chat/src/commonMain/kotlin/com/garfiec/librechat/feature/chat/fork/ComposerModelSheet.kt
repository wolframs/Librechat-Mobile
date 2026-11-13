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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.model.ModelRef
import com.garfiec.librechat.core.model.ParameterDefinition
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
    sections.mapIndexed { index, section ->
        (if (index == 0) listOf(ChatOverflowItem.TEXT) + section else section).filterNot { it in relocatedActions }
    }.filter { it.isNotEmpty() }

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
    val picks = (unstarredCurrent + ranked.take(6)).distinct()
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
    Box(modifier.height(48.dp).clickable { open = true }, contentAlignment = Alignment.CenterStart) {
        Row(Modifier.widthIn(max = 210.dp).height(36.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(shortModelName(state.selectedModel.orEmpty(),
                state.modelSpecs?.list?.firstOrNull { it.name == state.selectedSpec }?.label
                    ?: modelLabel.displayModel.takeIf { state.selectedEndpoint == "agents" }),
                Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (effort.isNotBlank()) {
                Text(effortLabel(effort), fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Icon(ForkIcons.ChevronDown, null, Modifier.size(16.dp))
        }
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
    val cacheRoute = com.garfiec.librechat.fork.cachettl.cacheTtlRoute(state.selectedEndpoint, state.selectedModel,
        state.endpointConfigs, state.modelParameters.dynamicValues, state.selectedAgentProvider,
        state.selection.selectedAgentModel, state.selection.selectedAgentReasoning)
    val api = koinInject<com.garfiec.librechat.fork.market.MarketApi>()
    var markets by remember(state.serverUrl) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(state.serverUrl) { markets = api.endpoints() }
    val parentPage = if (page in setOf("cache", "prices", "presets", "parameters")) "options" else "model"
    val priceEndpoint = state.selectedAgentProvider.takeIf { state.selectedEndpoint == "agents" } ?: state.selectedEndpoint
    val priceModel = state.selection.selectedAgentModel.takeIf { state.selectedEndpoint == "agents" } ?: state.selectedModel
    AdaptiveModalBottomSheet(onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { ComposerDragHandle() },
        modifier = Modifier.clearFocusOnTap()) {
        PlatformBackHandler(enabled = page != "model") { page = parentPage }
        if (page == "prices") {
            com.garfiec.librechat.fork.market.MarketPriceContent(state.serverUrl, priceModel.orEmpty(), onDismiss = dismiss)
            return@AdaptiveModalBottomSheet
        }
        if (page == "all") {
            ComposerAllModels(context, { page = "model" }, dismiss)
            return@AdaptiveModalBottomSheet
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ComposerSheetHeader(stringResource(when (page) {
                "options" -> Res.string.composer_options
                "cache" -> Res.string.composer_cache_window
                "effort" -> Res.string.composer_effort
                "presets" -> Res.string.composer_presets
                "parameters" -> Res.string.composer_parameters
                else -> Res.string.composer_model
            }), onNavigate = { if (page == "model") dismiss() else page = parentPage }, subPage = page != "model")
            when (page) {
                "cache" -> ComposerGroup {
                    listOf(null, "1h", "5m").forEachIndexed { index, value ->
                        ChoiceRow(stringResource(when (value) {
                            "1h" -> Res.string.composer_cache_hour
                            "5m" -> Res.string.composer_cache_five
                            else -> Res.string.composer_cache_default
                        }), null, vm.cacheTtl.arm == value) {
                            vm.cacheTtl.setArm(value, cacheRoute.canExtend)
                        }
                        if (index < 2) ComposerDivider()
                    }
                }
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
                    Text(stringResource(Res.string.composer_effort_detail), Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "presets" -> ComposerGroup {
                    ComposerAction(stringResource(Res.string.composer_load)) { dismiss(); context.load() }
                    ComposerDivider()
                    ComposerAction(stringResource(Res.string.composer_save)) { dismiss(); context.save() }
                }
                "model" -> {
                    val unstarredCurrent = current.takeIf { !it.starred }
                    val groups = listOfNotNull(
                        unstarredCurrent?.let { Res.string.composer_current to listOf(it) },
                        (if (candidates.any { it.starred }) Res.string.composer_favourites else Res.string.composer_most_used) to
                            picks.filter { it != unstarredCurrent },
                    )
                    groups.forEach { (heading, rows) ->
                        Text(stringResource(
                            heading),
                            Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (rows.isNotEmpty()) {
                            ComposerGroup {
                                rows.forEachIndexed { index, pick ->
                                    val selected = if (pick.spec != null) {
                                        state.selectedSpec == pick.spec
                                    } else {
                                        state.selectedSpec == null &&
                                            state.selectedEndpoint == pick.endpoint && state.selectedModel == pick.model
                                    }
                                    ChoiceRow(shortModelName(pick.model,
                                        state.modelSpecs?.list?.firstOrNull { it.name == pick.spec }?.label
                                            ?: pick.name.takeIf { pick.endpoint == "agents" }),
                                        pick.source, selected, model = true) {
                                        if (pick.spec != null) {
                                            state.modelSpecs?.list?.firstOrNull { it.name == pick.spec }?.let(vm::onSpecSelected)
                                        } else {
                                            vm.onModelSelected(pick.endpoint, pick.model)
                                        }
                                        dismiss()
                                    }
                                    if (index < rows.lastIndex) ComposerDivider()
                                }
                            }
                        }
                    }
                    ComposerGroup {
                        ComposerAction(stringResource(Res.string.composer_more)) { page = "all" }
                        if (definition != null) {
                            ComposerDivider()
                            ComposerAction(stringResource(Res.string.composer_effort), supporting =
                                effortLabel(state.selection.sendParameters.getValueForKey(definition.key)), trailingValue = true,
                            ) { page = "effort" }
                        }
                    }
                    ComposerGroup { ComposerAction(stringResource(Res.string.composer_options)) { page = "options" } }
                }
                "options" -> {
                    if (state.parametersEnabled || state.presetsEnabled || priceEndpoint in markets) {
                        ComposerGroup {
                            if (state.parametersEnabled) {
                                ComposerAction(stringResource(Res.string.composer_parameters)) {
                                    page = "parameters"
                                }
                            }
                            if (state.presetsEnabled) {
                                if (state.parametersEnabled) ComposerDivider()
                                ComposerAction(stringResource(Res.string.composer_presets)) {
                                    page = "presets"
                                }
                            }
                            if (priceEndpoint in markets && !priceModel.isNullOrBlank()) {
                                if (state.parametersEnabled || state.presetsEnabled) ComposerDivider()
                                ComposerAction(stringResource(Res.string.composer_prices)) { page = "prices" }
                            }
                        }
                    }
                    if (state.multiConvoEnabled && state.modelPickerShown) {
                        ComposerGroup {
                            ComposerAction(stringResource(Res.string.composer_compare)) {
                                dismiss()
                                when { state.comparisonState.isEnabled -> vm.stopComparison()
                                    state.comparisonState.isSettingUp -> vm.cancelComparisonSetup()
                                    else -> vm.beginComparisonSetup() }
                            }
                        }
                    }
                    if (cacheRoute.shown && cacheRoute.canExtend) {
                        ComposerGroup {
                            ComposerAction(stringResource(Res.string.composer_cache_window)) { page = "cache" }
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
    label: String, icon: ImageVector? = null, supporting: String? = null, trailingValue: Boolean = false, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (supporting.isNullOrBlank() || trailingValue) 56.dp else 64.dp)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
            if (!trailingValue && !supporting.isNullOrBlank()) {
                Text(supporting, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, overflow = TextOverflow.Clip)
            }
        }
        if (trailingValue && !supporting.isNullOrBlank()) {
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(ForkIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoiceRow(label: String, source: String?, checked: Boolean, enabled: Boolean = true, model: Boolean = false,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = if (source.isNullOrBlank()) 56.dp else 64.dp).clickable(enabled = enabled,
        onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, overflow = TextOverflow.Clip,
                fontWeight = if (model) FontWeight.Medium else FontWeight.Normal)
            if (!source.isNullOrBlank()) {
                Text(source,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, overflow = TextOverflow.Clip)
            }
        }
        if (checked) Icon(ForkIcons.Check, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun ComposerLeadingButtons(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        content()
        ComposerModelChip(Modifier.weight(1f))
        LocalComposer.current?.cache?.invoke()
    }
}

@Composable
internal fun composerCanFold(upstream: Boolean): Boolean = upstream && LocalComposer.current == null
