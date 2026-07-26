package com.garfiec.librechat.feature.agents.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Test

class AgentEditorDraftStateTest {

    @Test
    fun `unchanged editor leaves immediately but changed editor asks before leaving`() {
        val state = AgentEditorUiState()
        val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(state)
        val handle = AgentEditorStateHandle(
            stateFlow = stateFlow,
            scope = TestScope(),
            savedStateHandle = SavedStateHandle(),
            agentId = null,
        )
        handle.initializeDraftTracking(state)

        assertThat(handle.requestBack()).isTrue()

        handle.update { copy(name = "Unsaved agent") }

        assertThat(stateFlow.value.hasUnsavedChanges).isTrue()
        assertThat(handle.requestBack()).isFalse()
        assertThat(stateFlow.value.showDiscardConfirm).isTrue()
    }

    @Test
    fun `successful save clears dirty state and process-restoration payload`() {
        val savedState = SavedStateHandle()
        val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(AgentEditorUiState())
        val handle = AgentEditorStateHandle(
            stateFlow = stateFlow,
            scope = TestScope(),
            savedStateHandle = savedState,
            agentId = null,
        )
        handle.initializeDraftTracking(stateFlow.value)
        handle.update { copy(name = "Saved agent") }

        assertThat(savedState.get<String>(AgentEditorStateHandle.SAVED_DRAFT_KEY)).isNotNull()

        handle.markSaved()

        assertThat(stateFlow.value.hasUnsavedChanges).isFalse()
        assertThat(savedState.get<String>(AgentEditorStateHandle.SAVED_DRAFT_KEY)).isNull()
        assertThat(handle.requestBack()).isTrue()
    }

    @Test
    fun `saved-state recreation restores a multi-section draft`() {
        val firstSavedState = SavedStateHandle()
        val firstStateFlow = kotlinx.coroutines.flow.MutableStateFlow(
            AgentEditorUiState(isEditMode = true, agentId = "agent-a", name = "Server name"),
        )
        val firstHandle = AgentEditorStateHandle(
            stateFlow = firstStateFlow,
            scope = TestScope(),
            savedStateHandle = firstSavedState,
            agentId = "agent-a",
        )
        firstHandle.initializeDraftTracking(firstStateFlow.value)
        firstHandle.update {
            copy(
                name = "Draft name",
                instructions = "Preserve these instructions",
                selectedTools = listOf("web_search"),
                selectedMcpTools = setOf("filesystem/read"),
                fileSearchEnabled = true,
                selectedSubagentIds = listOf("agent-b"),
                chainAgentIds = listOf("agent-c"),
            )
        }
        val encoded = firstSavedState.get<String>(AgentEditorStateHandle.SAVED_DRAFT_KEY)

        val recreatedSavedState = SavedStateHandle(
            mapOf(AgentEditorStateHandle.SAVED_DRAFT_KEY to encoded),
        )
        val recreatedStateFlow = kotlinx.coroutines.flow.MutableStateFlow(
            AgentEditorUiState(isEditMode = true, agentId = "agent-a", name = "Server name"),
        )
        val recreatedHandle = AgentEditorStateHandle(
            stateFlow = recreatedStateFlow,
            scope = TestScope(),
            savedStateHandle = recreatedSavedState,
            agentId = "agent-a",
        )
        recreatedHandle.initializeDraftTracking(recreatedStateFlow.value)

        with(recreatedStateFlow.value) {
            assertThat(name).isEqualTo("Draft name")
            assertThat(instructions).isEqualTo("Preserve these instructions")
            assertThat(selectedTools).containsExactly("web_search")
            assertThat(selectedMcpTools).containsExactly("filesystem/read")
            assertThat(fileSearchEnabled).isTrue()
            assertThat(selectedSubagentIds).containsExactly("agent-b")
            assertThat(chainAgentIds).containsExactly("agent-c")
            assertThat(hasUnsavedChanges).isTrue()
        }
    }

    @Test
    fun `draft from another agent identity is discarded`() {
        val firstSavedState = SavedStateHandle()
        val firstStateFlow = kotlinx.coroutines.flow.MutableStateFlow(
            AgentEditorUiState(isEditMode = true, agentId = "agent-a", name = "Agent A"),
        )
        val firstHandle = AgentEditorStateHandle(
            stateFlow = firstStateFlow,
            scope = TestScope(),
            savedStateHandle = firstSavedState,
            agentId = "agent-a",
        )
        firstHandle.initializeDraftTracking(firstStateFlow.value)
        firstHandle.update { copy(name = "Wrong destination") }
        val encoded = firstSavedState.get<String>(AgentEditorStateHandle.SAVED_DRAFT_KEY)

        val secondSavedState = SavedStateHandle(
            mapOf(AgentEditorStateHandle.SAVED_DRAFT_KEY to encoded),
        )
        val secondStateFlow = kotlinx.coroutines.flow.MutableStateFlow(
            AgentEditorUiState(isEditMode = true, agentId = "agent-b", name = "Agent B"),
        )
        val secondHandle = AgentEditorStateHandle(
            stateFlow = secondStateFlow,
            scope = TestScope(),
            savedStateHandle = secondSavedState,
            agentId = "agent-b",
        )
        secondHandle.initializeDraftTracking(secondStateFlow.value)

        assertThat(secondStateFlow.value.name).isEqualTo("Agent B")
        assertThat(secondStateFlow.value.hasUnsavedChanges).isFalse()
        assertThat(secondSavedState.get<String>(AgentEditorStateHandle.SAVED_DRAFT_KEY)).isNull()
    }
}
