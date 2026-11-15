package com.garfiec.librechat.fork.resendfiles

import com.garfiec.librechat.core.model.Conversation
import com.garfiec.librechat.core.model.Preset
import com.garfiec.librechat.core.model.fork.ResendFiles
import com.garfiec.librechat.core.model.fork.resendFilesWire
import com.garfiec.librechat.core.ui.components.EndpointParameterRegistry
import com.garfiec.librechat.core.ui.components.ModelParameters
import com.garfiec.librechat.core.ui.fork.resendfiles.resendFilesOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResendFilesTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun stored(value: String) =
        json.decodeFromString<Conversation>("""{"conversationId":"c","resendFiles":$value}""").resendFiles

    @Test
    fun `a conversation's stored value reads as the server reads it`() {
        assertEquals(ResendFiles.ALL, stored("true"))
        assertEquals(ResendFiles.NONE, stored("false"))
        assertEquals(ResendFiles.NONE, stored("0"))
        assertEquals(ResendFiles(3), stored("3"))
        assertEquals(ResendFiles(2), stored("2.9"))
        assertEquals(ResendFiles.ALL, stored("\"3\""))
        assertNull(json.decodeFromString<Conversation>("""{"conversationId":"c"}""").resendFiles)
    }

    @Test
    fun `a preset is written back as true, false or the count`() {
        fun written(choice: ResendFiles) = json.encodeToString(Preset.serializer(), Preset(resendFiles = choice))
        assertEquals("""{"resendFiles":true}""", written(ResendFiles.ALL))
        assertEquals("""{"resendFiles":false}""", written(ResendFiles.NONE))
        assertEquals("""{"resendFiles":3}""", written(ResendFiles(3)))
    }

    @Test
    fun `the field's text maps to the choice and back`() {
        fun typed(text: String) = ModelParameters.DEFAULT.withUpdatedKey("resendFiles", text)
        assertEquals("", ModelParameters.DEFAULT.getValueForKey("resendFiles"))
        assertEquals(ResendFiles.ALL, resendFilesOf(typed("")))
        assertEquals(ResendFiles.NONE, resendFilesOf(typed("0")))
        assertEquals("0", typed("0").getValueForKey("resendFiles"))
        assertEquals("12", typed("12x").getValueForKey("resendFiles"))
        assertEquals(ResendFiles(ResendFiles.MAX), resendFilesOf(typed("99999")))
        // What an agent's stored parameters carry.
        assertEquals("", typed("3").withUpdatedKey("resendFiles", "true").getValueForKey("resendFiles"))
        assertEquals("0", typed("false").getValueForKey("resendFiles"))
        // Upstream's own off switch still reads as none.
        assertEquals("0", ModelParameters.DEFAULT.copy(resendFiles = false).getValueForKey("resendFiles"))
    }

    @Test
    fun `only the resend files key gets the count encoding`() {
        assertEquals(JsonPrimitive(false), resendFilesWire("resendFiles", "0"))
        assertEquals(JsonPrimitive(5), resendFilesWire("resendFiles", "5"))
        assertNull(resendFilesWire("maxContextTokens", "5"))
    }

    @Test
    fun `every parameter set offers the count field, unset by default`() {
        listOf("anthropic", "openAI", "google", "bedrock", "Some custom endpoint").forEach { endpoint ->
            val definition = EndpointParameterRegistry.getDefinitions(endpoint = endpoint, model = "anthropic.claude-3")
                .first { it.key == "resendFiles" }
            assertEquals("", definition.default, endpoint)
            assertEquals(false, ModelParameters.DEFAULT.valueDiffersFromDefault(definition), endpoint)
        }
    }
}
