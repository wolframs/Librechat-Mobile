package com.garfiec.librechat.fork.mcpcards

import com.garfiec.librechat.core.model.mcp.McpServer
import com.garfiec.librechat.core.model.mcp.McpTool

/** Presentation copies only: catalogue names fill missing descriptions without changing stored configs. */
fun withMcpCardSummaries(servers: List<McpServer>, tools: List<McpTool>): List<McpServer> {
    val toolsByServer = tools.groupBy { it.serverName }
    return servers.map { server ->
        if (!server.description.isNullOrBlank()) {
            server
        } else {
            val summary = toolsByServer[server.name].orEmpty()
                .map { it.name }
                .filter { it.isNotBlank() }
                .joinToString(", ")
            if (summary.isEmpty()) server else server.copy(description = summary)
        }
    }
}
