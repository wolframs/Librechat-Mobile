package com.garfiec.librechat.feature.chat.fork

import androidx.compose.runtime.Composable
import com.garfiec.librechat.feature.chat.components.parseToolIntent
import com.garfiec.librechat.feature.chat.components.parseToolName
import com.garfiec.librechat.feature.chat.components.preparingToolLabel
import com.garfiec.librechat.feature.chat.resources.Res
import com.garfiec.librechat.feature.chat.resources.tool_preparing
import com.garfiec.librechat.feature.chat.viewmodel.ActiveToolCall
import com.garfiec.librechat.fork.toollabels.toolCallTitle
import org.jetbrains.compose.resources.stringResource

/** Resolves presentation only; dispatch and output routing keep the original name. */
@Composable
internal fun toolCallLabel(name: String, intent: String? = null): String {
    val parsed = parseToolName(name)
    return toolCallTitle(
        toolName = parsed.toolName,
        server = parsed.mcpServer,
        friendlyName = parsed.friendlyName?.let { stringResource(it) },
        intent = intent,
    )
}

@Composable
internal fun streamingToolCallLabel(toolCall: ActiveToolCall): String {
    val intent = parseToolIntent(toolCall.input)
    val label = toolCallLabel(toolCall.name, intent)
    return if (intent == null && preparingToolLabel(toolCall) != null) {
        stringResource(Res.string.tool_preparing, label)
    } else {
        label
    }
}
