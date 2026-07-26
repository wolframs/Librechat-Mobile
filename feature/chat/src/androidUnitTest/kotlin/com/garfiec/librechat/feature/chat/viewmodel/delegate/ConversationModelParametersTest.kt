package com.garfiec.librechat.feature.chat.viewmodel.delegate

import com.garfiec.librechat.core.model.Conversation
import com.garfiec.librechat.core.ui.components.ModelParameters
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConversationModelParametersTest {

    @Test
    fun `hydrates Anthropic preset snapshot from an existing conversation`() {
        val parameters = Conversation(
            endpoint = "anthropic",
            model = "claude-opus-5",
            modelLabel = "Claude Opus Five",
            promptPrefix = "Persisted custom instructions",
            temperature = 0.8,
            topP = 0.7,
            topK = 5,
            maxContextTokens = 828400,
            maxOutputTokens = 8192,
            thinking = true,
            thinkingBudget = 2000,
            promptCache = true,
            promptCacheTtl = "1h",
            webSearch = true,
            resendFiles = true,
        ).toModelParameters()

        assertThat(parameters.customName).isEqualTo("Claude Opus Five")
        assertThat(parameters.customInstructions).isEqualTo("Persisted custom instructions")
        assertThat(parameters.temperature).isEqualTo(0.8f)
        assertThat(parameters.topP).isEqualTo(0.7f)
        assertThat(parameters.topK).isEqualTo(5)
        assertThat(parameters.maxContextTokens).isEqualTo(828400)
        assertThat(parameters.maxOutputTokens).isEqualTo(8192)
        assertThat(parameters.thinking).isTrue()
        assertThat(parameters.thinkingBudget).isEqualTo("2000")
        assertThat(parameters.dynamicValues["promptCache"]).isEqualTo("true")
        assertThat(parameters.dynamicValues["promptCacheTtl"]).isEqualTo("1h")
        assertThat(parameters.webSearch).isTrue()
        assertThat(parameters.resendFiles).isTrue()
    }

    @Test
    fun `explicit false and zero values override composer defaults`() {
        val parameters = Conversation(
            endpoint = "anthropic",
            model = "claude-opus-5",
            temperature = 0.0,
            topP = 0.0,
            thinking = false,
            thinkingBudget = 0,
            promptCache = false,
            webSearch = false,
            resendFiles = false,
        ).toModelParameters()

        assertThat(parameters.temperature).isEqualTo(0.0f)
        assertThat(parameters.topP).isEqualTo(0.0f)
        assertThat(parameters.thinking).isFalse()
        assertThat(parameters.thinkingBudget).isEqualTo("0")
        assertThat(parameters.dynamicValues["promptCache"]).isEqualTo("false")
        assertThat(parameters.webSearch).isFalse()
        assertThat(parameters.resendFiles).isFalse()
    }

    @Test
    fun `omitted Anthropic overrides resolve to endpoint defaults`() {
        val parameters = Conversation(
            endpoint = "anthropic",
            model = "claude-opus-5",
        ).toModelParameters()

        assertThat(parameters.temperature).isEqualTo(1.0f)
        assertThat(parameters.topP).isEqualTo(0.7f)
        assertThat(parameters.topK).isEqualTo(5)
        assertThat(parameters.thinking).isTrue()
        assertThat(parameters.thinkingBudget).isEqualTo("2000")
        assertThat(parameters.dynamicValues["promptCache"]).isEqualTo("true")
        assertThat(parameters.dynamicValues["promptCacheTtl"]).isEqualTo("5m")
    }

    @Test
    fun `omitted values retain composer defaults and aliases are supported`() {
        val parameters = Conversation(
            chatGptLabel = "Legacy custom name",
            system = "System instructions",
            maxTokens = 4096,
        ).toModelParameters()

        assertThat(parameters.customName).isEqualTo("Legacy custom name")
        assertThat(parameters.customInstructions).isEqualTo("System instructions")
        assertThat(parameters.maxOutputTokens).isEqualTo(4096)
        assertThat(parameters.temperature).isEqualTo(ModelParameters.DEFAULT.temperature)
        assertThat(parameters.topP).isEqualTo(ModelParameters.DEFAULT.topP)
    }
}
