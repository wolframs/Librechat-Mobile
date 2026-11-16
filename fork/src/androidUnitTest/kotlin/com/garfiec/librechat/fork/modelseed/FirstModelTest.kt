package com.garfiec.librechat.fork.modelseed

import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.endpoint.KeyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class FirstModelTest {
    private val models = linkedMapOf("openAI" to listOf("first"), "Surplus" to listOf("answer", "other"))
    private val gated = models.mapValues { EndpointConfig(userProvide = true) }
    private val stored = KeyState.Set(null, true, "never")

    @Test fun missingFirstKeyUsesStoredSecondKey() {
        val entry = firstModelEntry(models, gated, mapOf("openAI" to KeyState.Unset, "Surplus" to stored))
        assertEquals("Surplus", entry?.key)
        assertEquals("answer", entry?.value?.first())
    }

    @Test fun waitsForConfigAndEarlierKeyBeforeChoosingLaterRow() {
        assertNull(firstModelEntry(models, emptyMap(), emptyMap()))
        assertNull(firstModelEntry(models, gated, mapOf("Surplus" to stored)))
        assertNull(firstModelEntry(models, gated, mapOf("openAI" to KeyState.Loading, "Surplus" to stored)))
        assertEquals("openAI", firstModelEntry(models, gated, mapOf("openAI" to stored, "Surplus" to stored))?.key)
    }

    @Test fun keylessRowWinsInListOrderWithoutWaitingForLaterKeys() {
        val configs = gated + ("openAI" to EndpointConfig())
        assertEquals("openAI", firstModelEntry(models, configs, emptyMap())?.key)
        val reversed = models.entries.reversed().associate { it.toPair() }
        assertEquals("Surplus", firstModelEntry(reversed, configs, mapOf("Surplus" to stored))?.key)
    }

    @Test fun urlGateAndExpiredKeyAreBlockedAndNoneUsableKeepsSetupFallback() {
        val expired = KeyState.Expired(Instant.parse("2020-01-01T00:00:00Z"), "2020-01-01T00:00:00Z")
        val configs = gated + ("openAI" to EndpointConfig(userProvideURL = true))
        assertEquals("Surplus", firstModelEntry(models, configs, mapOf("openAI" to expired, "Surplus" to stored))?.key)
        assertEquals("openAI", firstModelEntry(models, configs, mapOf("openAI" to expired, "Surplus" to KeyState.Unset))?.key)
        assertNull(firstModelEntry(emptyMap(), configs, emptyMap()))
        assertEquals("Surplus", firstModelEntry(models + ("openAI" to emptyList()), configs, mapOf("Surplus" to stored))?.key)
    }
}
