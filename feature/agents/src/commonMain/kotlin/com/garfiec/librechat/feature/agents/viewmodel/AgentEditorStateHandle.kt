package com.garfiec.librechat.feature.agents.viewmodel

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Shared state accessor passed to all AgentEditorViewModel delegates.
 * Provides read/write access to the editor UI state and a coroutine scope.
 * Mirrors the chat feature's `ChatStateHandle` convention.
 */
class AgentEditorStateHandle(
    val stateFlow: MutableStateFlow<AgentEditorUiState>,
    val scope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    agentId: String?,
) {
    private val draftIdentity = agentId ?: NEW_AGENT_IDENTITY
    private var baseline: AgentEditorDraft? = null

    val state: AgentEditorUiState get() = stateFlow.value

    fun update(transform: AgentEditorUiState.() -> AgentEditorUiState) {
        stateFlow.update { current ->
            withDirtyState(current.transform())
        }
    }

    /**
     * Establishes the clean server/default baseline and overlays a process-restored
     * draft only when it belongs to this exact create/edit destination.
     */
    fun initializeDraftTracking(cleanState: AgentEditorUiState) {
        baseline = cleanState.toDraft()
        val restoredDraft = restoreDraft()
        val restoredState = restoredDraft?.applyTo(cleanState) ?: cleanState
        stateFlow.value = withDirtyState(restoredState)
    }

    /** Marks the current content clean after a successful server mutation. */
    fun markSaved() {
        baseline = state.toDraft()
        clearStoredDraft()
        stateFlow.update {
            it.copy(hasUnsavedChanges = false, showDiscardConfirm = false)
        }
    }

    /** Replaces the form after a server-side revert and intentionally ignores any older draft. */
    fun replaceWithCleanState(cleanState: AgentEditorUiState) {
        clearStoredDraft()
        baseline = cleanState.toDraft()
        stateFlow.value = cleanState.copy(
            hasUnsavedChanges = false,
            showDiscardConfirm = false,
        )
    }

    /** Returns true when navigation may proceed immediately. */
    fun requestBack(): Boolean {
        if (!state.hasUnsavedChanges) return true
        stateFlow.update { it.copy(showDiscardConfirm = true) }
        return false
    }

    fun dismissDiscardConfirmation() {
        stateFlow.update { it.copy(showDiscardConfirm = false) }
    }

    fun discardDraft() {
        clearStoredDraft()
        stateFlow.update {
            it.copy(hasUnsavedChanges = false, showDiscardConfirm = false)
        }
    }

    private fun withDirtyState(state: AgentEditorUiState): AgentEditorUiState {
        val cleanDraft = baseline ?: return state.copy(hasUnsavedChanges = false)
        val draft = state.toDraft()
        val isDirty = draft != cleanDraft
        if (isDirty) {
            savedStateHandle[SAVED_DRAFT_KEY] = draftJson.encodeToString(
                StoredAgentEditorDraft(identity = draftIdentity, draft = draft),
            )
        } else {
            clearStoredDraft()
        }
        return state.copy(hasUnsavedChanges = isDirty)
    }

    private fun restoreDraft(): AgentEditorDraft? {
        val encoded = savedStateHandle.get<String>(SAVED_DRAFT_KEY) ?: return null
        return try {
            draftJson.decodeFromString<StoredAgentEditorDraft>(encoded)
                .takeIf { it.identity == draftIdentity }
                ?.draft
                .also { if (it == null) clearStoredDraft() }
        } catch (_: SerializationException) {
            clearStoredDraft()
            null
        } catch (_: IllegalArgumentException) {
            clearStoredDraft()
            null
        }
    }

    private fun clearStoredDraft() {
        savedStateHandle.remove<String>(SAVED_DRAFT_KEY)
    }

    internal companion object {
        const val SAVED_DRAFT_KEY = "agent_editor_draft"
        private const val NEW_AGENT_IDENTITY = "new"
        private val draftJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
