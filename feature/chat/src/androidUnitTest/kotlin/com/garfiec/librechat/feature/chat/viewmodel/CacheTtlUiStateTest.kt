package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.ui.components.ModelParameters
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CacheTtlUiStateTest {

    @Test
    fun `cache rail is enabled for direct Anthropic chats by default`() {
        val state = ChatUiState(
            selection = ModelSelectionState(
                selectedEndpoint = "anthropic",
                selectedModel = "claude-opus-5",
            ),
        )

        assertThat(state.cacheTtlEnabled).isTrue()
    }

    @Test
    fun `explicitly disabled prompt caching hides the rail`() {
        val state = ChatUiState(
            selection = ModelSelectionState(
                selectedEndpoint = "anthropic",
                selectedModel = "claude-opus-5",
                modelParameters = ModelParameters.DEFAULT.copy(
                    dynamicValues = mapOf("promptCache" to "false"),
                ),
            ),
        )

        assertThat(state.cacheTtlEnabled).isFalse()
    }

    @Test
    fun `non-Anthropic endpoint never exposes the cache rail`() {
        val state = ChatUiState(
            selection = ModelSelectionState(
                selectedEndpoint = "openAI",
                selectedModel = "gpt-5",
                modelParameters = ModelParameters.DEFAULT.copy(
                    dynamicValues = mapOf("promptCache" to "true"),
                ),
            ),
        )

        assertThat(state.cacheTtlEnabled).isFalse()
    }
}
