package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.common.ToolConstants
import com.garfiec.librechat.feature.chat.model.McpServerDisplayData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChatRequestBuilderToolValidationTest {

    @Test
    fun staleMcpServerNamesAreNotSerialized() {
        val state = ChatUiState(
            selection = ModelSelectionState(
                selectedEndpoint = "anthropic",
                selectedMcpServerNames = setOf("available", "removed"),
                mcpServers = listOf(mcp("available")),
            ),
        )

        val ephemeral = ChatRequestBuilder { state }.buildEphemeralAgent()

        assertThat(ephemeral?.mcp).containsExactly("available")
    }

    @Test
    fun onlyStaleMcpSelectionProducesNoEphemeralAgent() {
        val state = ChatUiState(
            selection = ModelSelectionState(
                selectedEndpoint = "anthropic",
                selectedMcpServerNames = setOf("removed"),
                mcpServers = listOf(mcp("available")),
            ),
        )

        assertThat(ChatRequestBuilder { state }.buildEphemeralAgent()).isNull()
    }

    @Test
    fun serverGatesPreventUnavailableToolsFromBeingSerialized() {
        val state = ChatUiState(
            selection = ModelSelectionState(
                selectedEndpoint = "anthropic",
                enabledTools = setOf(
                    ToolConstants.FILE_SEARCH,
                    ToolConstants.CODE_INTERPRETER,
                ),
            ),
            gates = FeatureGatesState(
                fileSearchEnabled = false,
                runCodeEnabled = false,
            ),
        )

        assertThat(ChatRequestBuilder { state }.buildEphemeralAgent()).isNull()
    }

    private fun mcp(name: String) = McpServerDisplayData(
        name = name,
        title = null,
        description = null,
        isConnected = true,
    )
}
