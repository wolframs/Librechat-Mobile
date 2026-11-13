package com.garfiec.librechat.fork.mcp

/** Null opt-outs mean an existing chat whose server state has not been learned yet. */
data class ConnectorState(
    val off: Set<String>? = null,
    val ready: Boolean = true,
    val touched: Boolean = false,
    val edited: Boolean = false,
)

fun selectedConnectors(selected: Set<String>, defaults: Set<String>, off: Set<String>?): Set<String> =
    (selected - defaults) + (if (off == null) emptySet() else defaults - off)

fun toggledSwitchOffs(off: Set<String>?, server: String): Set<String>? =
    off?.let { if (server in it) it - server else it + server }

fun requestSwitchOffs(off: Set<String>?): List<String>? = off?.toList()
