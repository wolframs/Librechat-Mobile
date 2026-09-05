package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.model.CustomEndpointParams
import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.response.UploadRoute
import com.garfiec.librechat.core.ui.components.EndpointParameterRegistry
import com.garfiec.librechat.core.ui.components.ModelParameters
import com.garfiec.librechat.feature.chat.model.McpServerDisplayData
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class GatewayCompatibilityTest {
    private val gateway = "Renamed Claude gateway"
    private val config = EndpointConfig(type = "custom", provider = "anthropic", extendedCacheTTL = false)
    private fun state(config: EndpointConfig = this.config) = ChatUiState(
        selection = ModelSelectionState(
            selectedEndpoint = gateway,
            selectedModel = "claude-opus-4.8",
            endpointConfigs = mapOf(gateway to config),
        ),
        composer = ComposerState(armedCacheTtl = CacheTtl.ONE_HOUR),
    )

    @Test fun gatewayUsesProviderIdentityAndClampsAnOldOneHourArm() {
        val state = state()
        assertThat(state.cacheTtlEnabled).isTrue()
        assertThat(state.extendedCacheTtlEnabled).isFalse()
        assertThat(state.outgoingCacheTtl).isEqualTo(CacheTtl.FIVE_MINUTES)
        val definitions = EndpointParameterRegistry.getDefinitions(gateway, endpointConfig = config)
        assertThat(definitions.map { it.key }).contains("thinking")
        assertThat(definitions.first { it.key == "promptCacheTtl" }.readOnly).isTrue()
    }

    @Test fun BorrowedParameterPanelDoesNotClaimAnthropicRouting() {
        val borrowed = EndpointConfig(type = "custom", customParams = CustomEndpointParams("anthropic"))
        assertThat(state(borrowed).cacheTtlEnabled).isFalse()
        assertThat(state(borrowed).outgoingCacheTtl).isNull()
        assertThat(EndpointParameterRegistry.getDefinitions(gateway, endpointConfig = borrowed).map { it.key })
            .contains("thinking")
    }

    @Test fun onlyExplicitlySupportedCustomEndpointMayArmOneHour() {
        assertThat(state(config.copy(extendedCacheTTL = null)).extendedCacheTtlEnabled).isFalse()
        assertThat(state(config.copy(extendedCacheTTL = true)).outgoingCacheTtl).isEqualTo(CacheTtl.ONE_HOUR)
    }

    @Test fun typedYamlOverridesLockControlsAndAreNotSentFromStalePreferences() {
        val override = Json.parseToJsonElement("""{"key":"promptCache","default":false,"readonly":true}""").jsonObject
        val cfg = config.copy(customParams = CustomEndpointParams("anthropic", listOf(override)))
        val params = ModelParameters.DEFAULT.copy(maxOutputTokens = 8192, dynamicValues = mapOf(
            "promptCache" to "true", "promptCacheTtl" to "1h", "thinking" to "false",
        ))
        val payload = ModelParamPayload.build(gateway, null, "claude-opus-4.8", false, params, cfg)
        assertThat(payload.keys).doesNotContain("promptCache")
        assertThat(payload.keys).doesNotContain("promptCacheTtl")
        assertThat(payload["maxOutputTokens"]).isEqualTo(kotlinx.serialization.json.JsonPrimitive("8192"))
        assertThat(state(cfg).cacheTtlEnabled).isFalse()
    }

    @Test fun bothUserScopedMcpsAreSentWithoutRequiringAnAgentOrAToolCatalogue() {
        val names = setOf("openrouter-imager", "audio-ears")
        val base = state()
        val withMcp = base.copy(selection = base.selection.copy(
            selectedMcpServerNames = names,
            mcpServers = names.map { McpServerDisplayData(it, null, null, false) },
        ))
        assertThat(ChatRequestBuilder { withMcp }.buildEphemeralAgent()?.mcp).containsExactlyElementsIn(names)
    }

    @Test fun audioStaysNativeForTheBackendAndAudioEarsToConsume() {
        for (type in listOf("audio/mpeg", "audio/wav", "audio/flac", "audio/mp4", "audio/ogg")) {
            assertThat(state().uploadRouteFor(type)).isEqualTo(UploadRoute.PROVIDER)
        }
    }
}
