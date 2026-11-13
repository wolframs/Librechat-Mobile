package com.garfiec.librechat.fork.cachettl

import com.garfiec.librechat.core.data.fork.cachettl.CacheTtlMarker
import com.garfiec.librechat.core.model.CustomParams
import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.Message
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CacheTtlRulesTest {
    private fun route(endpoint: String, model: String? = "claude-sonnet", config: EndpointConfig? = null,
        params: Map<String, String> = emptyMap(), provider: String? = null, agentParams: Map<String, String> = emptyMap()) =
        cacheTtlRoute(endpoint, model, config?.let { mapOf(endpoint to it) }.orEmpty(), params, provider, model, agentParams)

    @Test fun visibilityAndMeasuredExtension() {
        assertTrue(route("anthropic").canExtend)
        assertFalse(route("anthropic", params = mapOf("promptCache" to "false")).shown)
        assertFalse(route("openAI").shown)
        assertTrue(route("custom", config = EndpointConfig(provider = "anthropic")).shown)
        assertFalse(route("custom", config = EndpointConfig(provider = "anthropic")).canExtend)
        assertTrue(route("custom", config = EndpointConfig(provider = "anthropic", extendedCacheTTL = true)).canExtend)
        val router = EndpointConfig(customParams = CustomParams(defaultParamsEndpoint = "openrouter"))
        assertTrue(route("router", "CLAUDE-sonnet", router).shown)
        assertFalse(route("router", "gpt-4", router).shown)
        assertFalse(route("router", "claude", router).canExtend)
        assertFalse(route("openAI", config = EndpointConfig(extendedCacheTTL = true)).shown)
    }

    @Test fun agentsUseSavedParametersAndDestination() {
        assertTrue(route("agents", provider = "anthropic", params = mapOf("promptCache" to "false")).canExtend)
        assertFalse(route("agents", provider = "anthropic", agentParams = mapOf("promptCache" to "false")).shown)
        assertFalse(route("agents", provider = null).shown)
        assertEquals(mapOf("promptCache" to "false"), agentCacheParameters(Json.parseToJsonElement("""{"promptCache":false}""")))
    }

    @Test fun countdownBoundaries() {
        assertEquals("59m", formatRemaining(58 * 60_000L + 1))
        assertEquals("11m", formatRemaining(600_001))
        assertEquals("10:00", formatRemaining(600_000))
        assertEquals("0:01", formatRemaining(1))
        assertEquals("0:00", formatRemaining(-1))
        assertEquals(300_000L, remainingMillis(CacheTtlMarker("a", "5m", 1000), 1000))
        assertEquals(3_600_000L, remainingMillis(CacheTtlMarker("a", "1h", 1000), 1000))
    }

    @Test fun oneShotCycleQueueAndDestinationChange() {
        val session = CacheTtlSession()
        val capable = route("anthropic")
        session.cycle(true)
        assertEquals("1h", session.arm)
        val base = JsonObject(emptyMap())
        assertSame(base, session.payload(base, capable, direct = false))
        assertEquals("1h", session.arm)
        assertEquals("\"1h\"", session.payload(base, capable, direct = true)?.get("cacheTTL").toString())
        assertNull(session.arm)
        assertSame(base, session.payload(base, capable, direct = true))
        session.cycle(true); session.cycle(true)
        assertEquals("5m", session.arm)
        session.cycle(true)
        assertNull(session.arm)
        session.cycle(true)
        assertSame(base, session.payload(base, route("openAI"), true))
        assertNull(session.arm)
    }

    @Test fun anchorUsesDisplayedBranchAndSkipsErrorsUsersAndUnmarkedReplies() {
        fun message(id: String, user: Boolean = false, error: Boolean = false) =
            Message(messageId = id, conversationId = "c", isCreatedByUser = user, error = error)
        val first = CacheTtlMarker("a", "5m", 1000)
        val markers = listOf(
            first, CacheTtlMarker("sibling", "1h", 9000),
            CacheTtlMarker("error", "1h", 5000), CacheTtlMarker("user", "1h", 6000),
        )
        val path =
            listOf(message("a"), message("error", error = true), message("user", user = true), message("unmarked"))
        assertEquals(first, cacheTtlAnchor(path, markers))
        assertNull(cacheTtlAnchor(listOf(message("unmarked")), markers))
    }
}
