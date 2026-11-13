package com.garfiec.librechat.fork.mcp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConnectorRulesTest {
    @Test fun selectionCombinesGlobalAndChatDefaults() {
        assertEquals(setOf("imager", "optmem"), selectedConnectors(setOf("imager", "web"), setOf("web", "optmem"), setOf("web")))
        assertEquals(setOf("web", "optmem"), selectedConnectors(emptySet(), setOf("web", "optmem"), emptySet()))
        assertEquals(setOf("imager"), selectedConnectors(setOf("imager", "web"), setOf("web"), null))
    }

    @Test fun togglePreservesOtherSwitchOffsAndUnknownState() {
        assertEquals(setOf("web", "optmem"), toggledSwitchOffs(setOf("web"), "optmem"))
        assertEquals(setOf("web"), toggledSwitchOffs(setOf("web", "optmem"), "optmem"))
        assertNull(toggledSwitchOffs(null, "web"))
    }

    @Test fun requestDistinguishesUnknownFromKnownEmpty() {
        assertNull(requestSwitchOffs(null))
        assertEquals(emptyList(), requestSwitchOffs(emptySet()))
        assertEquals(listOf("web"), requestSwitchOffs(setOf("web")))
    }
}
