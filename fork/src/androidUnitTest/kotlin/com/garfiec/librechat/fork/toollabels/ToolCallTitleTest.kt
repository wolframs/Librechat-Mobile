package com.garfiec.librechat.fork.toollabels

import org.junit.Test
import kotlin.test.assertEquals

class ToolCallTitleTest {
    @Test fun connectorNamesKeepTheirToolHalf() {
        assertEquals("web · fetch", toolCallTitle("fetch", "web", null, null))
        assertEquals("imager · generate_image", toolCallTitle("generate_image", "imager", null, null))
        assertEquals("web · web_fetch", toolCallTitle("web_fetch", "web", null, null))
    }

    @Test fun validatedIntentWinsOverConnectorAndNativeNames() {
        assertEquals("Read the page", toolCallTitle("fetch", "web", null, "Read the page"))
        assertEquals("Look it up", toolCallTitle("web_search", null, "Search the web", "Look it up"))
    }

    @Test fun nativeNamesUseTheSuppliedLocaleAndUnknownNamesStayRaw() {
        assertEquals("Search the web", toolCallTitle("web_search", null, "Search the web", null))
        assertEquals("Websuche", toolCallTitle("web_search", null, "Websuche", null))
        assertEquals("unknown_tool", toolCallTitle("unknown_tool", null, null, null))
    }

    @Test fun connectorCannotBorrowANativeFriendlyName() {
        assertEquals("web · web_search", toolCallTitle("web_search", "web", "Search the web", null))
    }
}
