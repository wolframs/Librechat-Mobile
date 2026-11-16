package com.garfiec.librechat.fork.mcpcards

import com.garfiec.librechat.core.model.mcp.McpServer
import com.garfiec.librechat.core.model.mcp.McpTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class McpCardSummaryTest {
    @Test fun nonblankDescriptionWinsAndOriginalConfigStaysUnchanged() {
        val described = McpServer(name = "described", description = " Server description ")
        val missing = McpServer(name = "missing")
        val rows = withMcpCardSummaries(
            listOf(described, missing),
            listOf(McpTool("ignored", serverName = "described"), McpTool("fetch", serverName = "missing")),
        )

        assertSame(described, rows[0])
        assertEquals(" Server description ", rows[0].description)
        assertEquals("fetch", rows[1].description)
        assertEquals(null, missing.description)
    }

    @Test fun blankDescriptionsUseOnlyTheirCatalogueNamesAndMissingToolsKeepEmptySlot() {
        val servers = listOf(
            McpServer(name = "images", description = " \n "),
            McpServer(name = "empty"),
            McpServer(name = "blank-tools", description = " "),
        )
        val rows = withMcpCardSummaries(
            servers,
            listOf(
                McpTool("get_images", serverName = "images"),
                McpTool("fetch", serverName = "other"),
                McpTool("generate_image", serverName = "images"),
                McpTool("unassigned"),
                McpTool(" ", serverName = "blank-tools"),
            ),
        )

        assertEquals("get_images, generate_image", rows[0].description)
        assertSame(servers[1], rows[1])
        assertSame(servers[2], rows[2])
        assertSame(servers[0], withMcpCardSummaries(servers, emptyList())[0])
    }
}
