package com.garfiec.librechat.feature.chat.viewmodel.delegate

import com.garfiec.librechat.core.common.network.ConnectivityObserver
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.data.repository.AgentRepository
import com.garfiec.librechat.core.data.repository.ConfigRepository
import com.garfiec.librechat.core.data.repository.McpRepository
import com.garfiec.librechat.core.data.util.PermissionGate
import com.garfiec.librechat.core.model.Conversation
import com.garfiec.librechat.feature.chat.viewmodel.ChatStateHandle
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState
import com.garfiec.librechat.feature.chat.viewmodel.ModelSelectionHandle
import com.garfiec.librechat.feature.chat.viewmodel.ModelSelectionState
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConversationSelectionHydrationTest {

    @Test
    fun `applying existing conversation updates model and parameters atomically`() = runTest {
        val state = MutableStateFlow(
            ChatUiState(
                selection = ModelSelectionState(
                    selectedEndpoint = "openAI",
                    selectedModel = "gpt-4o",
                ),
            ),
        )
        val rootHandle = ChatStateHandle(stateFlow = state, scope = this)
        val connectivity = mockk<ConnectivityObserver>(relaxed = true).also {
            every { it.isConnected } returns MutableStateFlow(true)
        }
        val delegate = ModelSelectionDelegate(
            handle = ModelSelectionHandle(rootHandle),
            configRepository = mockk<ConfigRepository>(relaxed = true),
            agentRepository = mockk<AgentRepository>(relaxed = true),
            mcpRepository = mockk<McpRepository>(relaxed = true),
            settingsDataStore = mockk<SettingsDataStore>(relaxed = true),
            permissionGate = mockk<PermissionGate>(relaxed = true),
            connectivityObserver = connectivity,
        )

        val applied = delegate.applyConversationModel(
            Conversation(
                endpoint = "anthropic",
                model = "claude-opus-5",
                modelLabel = "Claude Opus Five",
                promptPrefix = "Persisted custom instructions",
                maxContextTokens = 828400,
                promptCacheTtl = "1h",
                webSearch = true,
            ),
        )

        assertThat(applied).isTrue()
        assertThat(state.value.selectedEndpoint).isEqualTo("anthropic")
        assertThat(state.value.selectedModel).isEqualTo("claude-opus-5")
        assertThat(state.value.modelParameters.customName).isEqualTo("Claude Opus Five")
        assertThat(state.value.modelParameters.customInstructions).isEqualTo("Persisted custom instructions")
        assertThat(state.value.modelParameters.maxContextTokens).isEqualTo(828400)
        assertThat(state.value.modelParameters.dynamicValues["promptCacheTtl"]).isEqualTo("1h")
        assertThat(state.value.modelParameters.webSearch).isTrue()
        assertThat(delegate.conversationModelLoaded).isTrue()
        assertThat(delegate.conversationModelResolved).isTrue()
    }
}
