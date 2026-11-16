package com.garfiec.librechat.fork.toollabels

/** Intent is already validated against the first argument key by the caller's parser. */
fun toolCallTitle(toolName: String, server: String?, friendlyName: String?, intent: String?): String =
    intent ?: server?.let { "$it · $toolName" } ?: friendlyName ?: toolName
