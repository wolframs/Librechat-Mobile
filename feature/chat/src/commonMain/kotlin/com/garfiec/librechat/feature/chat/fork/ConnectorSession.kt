package com.garfiec.librechat.feature.chat.fork

import com.garfiec.librechat.core.model.Conversation
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState
import com.garfiec.librechat.feature.chat.viewmodel.ModelSelectionHandle
import com.garfiec.librechat.feature.chat.viewmodel.ModelSelectionState
import com.garfiec.librechat.fork.mcp.ConnectorState
import com.garfiec.librechat.fork.mcp.ConnectorStore
import com.garfiec.librechat.fork.mcp.toggledSwitchOffs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun initialConnectorState(chat: String?): ChatUiState = ChatUiState(
    selection = ModelSelectionState(connectors = ConnectorState(off = if (chat == null) emptySet() else null, ready = false)),
)

/** A picker edit made while disk was loading outranks the startup snapshot. */
fun restoreConnectors(state: ChatUiState, servers: Set<String>, tools: Set<String>): ChatUiState = state.copy(
    selection = state.selection.copy(
        selectedMcpServerNames = if (state.selection.connectors.touched) state.selectedMcpServerNames else servers,
        enabledTools = tools,
        connectors = state.selection.connectors.copy(ready = true),
    ),
)

fun toggleDefaultConnector(handle: ModelSelectionHandle, name: String): Boolean {
    if (handle.state.mcpServers.none { it.name == name && it.defaultOn }) return false
    handle.update {
        selection = selection.copy(connectors = selection.connectors.copy(
            off = toggledSwitchOffs(selection.connectors.off, name), edited = selection.connectors.off != null,
        ))
    }
    return true
}

suspend fun receiveConnectorState(
    state: MutableStateFlow<ChatUiState>, conversation: Conversation?, store: ConnectorStore?,
    account: String?, scope: CoroutineScope,
) {
    val chat = conversation?.conversationId ?: return
    val off = conversation.mcpOff?.toSet() ?: account?.let { store?.read(it, chat) } ?: return
    state.update { current ->
        if (current.selection.connectors.edited) {
            current
        } else {
            current.copy(selection = current.selection.copy(connectors = current.selection.connectors.copy(off = off)))
        }
    }
    if (account != null && conversation.mcpOff != null) scope.launch { store?.remember(account, chat, off) }
}

fun effectiveConnectors(selection: ModelSelectionState): Set<String> {
    val selected = if (selection.selectedSpec == null) {
        selection.selectedMcpServerNames
    } else {
        val added = selection.specTools?.takeIf { it.spec == selection.selectedSpec && !selection.toolsHiddenBySpec }
        selection.lockedMcpServers + added?.addedMcp.orEmpty()
    }
    val effective = com.garfiec.librechat.fork.mcp.selectedConnectors(
        selected, selection.mcpServers.filter { it.defaultOn }.map { it.name }.toSet(), selection.connectors.off,
    )
    // Upstream's rule: outside a spec, a server the chat does not offer is never in use.
    if (selection.selectedSpec != null) return effective
    return effective - selection.mcpServers.filterNot { it.chatSelectable }.map { it.name }.toSet()
}

fun seedConnectorHandoff(state: ChatUiState, off: Set<String>?): ChatUiState = state.copy(
    selection = state.selection.copy(connectors = state.selection.connectors.copy(off = off, edited = true)),
)

suspend fun rememberConnectorFinal(store: ConnectorStore?, conversation: Conversation, account: String?) {
    val chat = conversation.conversationId ?: return
    if (account != null) store?.remember(account, chat, conversation.mcpOff.orEmpty().toSet())
}
