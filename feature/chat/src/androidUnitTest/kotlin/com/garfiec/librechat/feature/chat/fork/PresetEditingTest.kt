package com.garfiec.librechat.feature.chat.fork

import com.garfiec.librechat.core.model.Preset
import com.garfiec.librechat.core.ui.components.ModelParameters
import com.garfiec.librechat.feature.chat.util.ParameterTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PresetEditingTest {
    private fun target(endpoint: String) = ParameterTarget(endpoint, null, "m", endpoint, null)

    private val stored = Preset(
        presetId = "p1", title = "Old", endpoint = "anthropic", model = "claude-x", spec = "s",
        temperature = 0.7, topPCamel = 0.9, system = "Be brief.", modelLabel = "Brief", resendFiles = false,
        promptCache = true, promptCacheTtl = "1h", imageDetail = "high",
    )

    @Test
    fun `an unchanged edit gives back what was stored, as a new preset`() {
        val edited = stored.edited("Old", "anthropic", "claude-x", stored.editorParameters(), target("anthropic"))
        assertEquals(stored.copy(presetId = null, chatGptLabel = "Brief", promptPrefix = "Be brief."), edited)
    }

    @Test
    fun `a reset value is cleared and the spelling follows the new parameter set`() {
        val params = stored.editorParameters().copy(temperature = ModelParameters.DEFAULT.temperature)
        val edited = stored.edited("New", "openAI", "gpt-x", params, target("openAI"))
        assertNull(edited.temperature)
        assertEquals(0.9, edited.topPSnake)
        assertNull(edited.topPCamel)
        assertNull(edited.spec)
        assertEquals("New", edited.title)
    }
}
