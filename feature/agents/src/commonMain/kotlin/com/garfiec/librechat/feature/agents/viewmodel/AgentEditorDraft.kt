package com.garfiec.librechat.feature.agents.viewmodel

import com.garfiec.librechat.core.model.HandoffEdge
import com.garfiec.librechat.feature.agents.components.model.AgentAdvancedSettings
import com.garfiec.librechat.feature.agents.components.model.AgentCapabilities
import com.garfiec.librechat.feature.agents.components.model.AgentSharingState
import com.garfiec.librechat.feature.agents.components.model.SupportContactState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Restorable, user-editable content from the Agent editor.
 *
 * Reference catalogs, loading flags, validation errors, dialogs, and server-owned
 * action/file mutations are deliberately excluded. This keeps dirty detection stable
 * while background data loads and prevents transient UI state from surviving process
 * recreation.
 */
@Serializable
internal data class AgentEditorDraft(
    val name: String,
    val description: String,
    val instructions: String,
    val model: String,
    val provider: String,
    val category: String,
    val selectedTools: List<String>,
    val conversationStarters: List<String>,
    val capabilities: AgentCapabilities,
    val advancedSettings: AgentAdvancedSettings,
    val selectedMcpTools: Set<String>,
    val codeInterpreterEnabled: Boolean,
    val fileSearchEnabled: Boolean,
    val webSearchEnabled: Boolean,
    val fileContextEnabled: Boolean,
    val skillsEnabled: Boolean,
    val selectedSkillIds: List<String>,
    val subagentsEnabled: Boolean,
    val subagentAllowSelf: Boolean,
    val selectedSubagentIds: List<String>,
    val sharingState: AgentSharingState,
    val chainAgentIds: List<String>,
    val handoffEdges: List<HandoffEdge>,
    val unparsedHandoffEdges: List<JsonElement>,
    val supportContact: SupportContactState,
    val toolOptions: JsonObject?,
    val additionalInstructions: String?,
    val toolKwargs: JsonElement?,
)

@Serializable
internal data class StoredAgentEditorDraft(
    val identity: String,
    val draft: AgentEditorDraft,
)

internal fun AgentEditorUiState.toDraft(): AgentEditorDraft = AgentEditorDraft(
    name = name,
    description = description,
    instructions = instructions,
    model = model,
    provider = provider,
    category = category,
    selectedTools = selectedTools,
    conversationStarters = conversationStarters,
    capabilities = capabilities,
    advancedSettings = advancedSettings,
    selectedMcpTools = selectedMcpTools,
    codeInterpreterEnabled = codeInterpreterEnabled,
    fileSearchEnabled = fileSearchEnabled,
    webSearchEnabled = webSearchEnabled,
    fileContextEnabled = fileContextEnabled,
    skillsEnabled = skillsEnabled,
    selectedSkillIds = selectedSkillIds,
    subagentsEnabled = subagentsEnabled,
    subagentAllowSelf = subagentAllowSelf,
    selectedSubagentIds = selectedSubagentIds,
    sharingState = sharingState,
    chainAgentIds = chainAgentIds,
    handoffEdges = handoffEdges,
    unparsedHandoffEdges = unparsedHandoffEdges,
    supportContact = supportContact,
    toolOptions = toolOptions,
    additionalInstructions = additionalInstructions,
    toolKwargs = toolKwargs,
)

internal fun AgentEditorDraft.applyTo(state: AgentEditorUiState): AgentEditorUiState = state.copy(
    name = name,
    description = description,
    instructions = instructions,
    model = model,
    provider = provider,
    category = category,
    selectedTools = selectedTools,
    conversationStarters = conversationStarters,
    capabilities = capabilities,
    advancedSettings = advancedSettings,
    selectedMcpTools = selectedMcpTools,
    codeInterpreterEnabled = codeInterpreterEnabled,
    fileSearchEnabled = fileSearchEnabled,
    webSearchEnabled = webSearchEnabled,
    fileContextEnabled = fileContextEnabled,
    skillsEnabled = skillsEnabled,
    selectedSkillIds = selectedSkillIds,
    subagentsEnabled = subagentsEnabled,
    subagentAllowSelf = subagentAllowSelf,
    selectedSubagentIds = selectedSubagentIds,
    sharingState = sharingState,
    chainAgentIds = chainAgentIds,
    handoffEdges = handoffEdges,
    unparsedHandoffEdges = unparsedHandoffEdges,
    supportContact = supportContact,
    toolOptions = toolOptions,
    additionalInstructions = additionalInstructions,
    toolKwargs = toolKwargs,
)
