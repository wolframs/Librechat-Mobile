package com.garfiec.librechat.fork.chatimport

import androidx.compose.runtime.Composable

data class ImportDocument(val name: String, val read: suspend () -> ByteArray?)

@Composable
expect fun rememberChatImportPicker(onPick: (ImportDocument) -> Unit): (() -> Unit)?
