package com.garfiec.librechat.fork.chatimport

import androidx.compose.runtime.Composable

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun rememberChatImportPicker(onPick: (ImportDocument) -> Unit): (() -> Unit)? = null
