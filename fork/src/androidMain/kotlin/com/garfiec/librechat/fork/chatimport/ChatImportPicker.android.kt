package com.garfiec.librechat.fork.chatimport

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** OpenDocument accepts every MIME type because providers often mistype Markdown files. */
@Composable
actual fun rememberChatImportPicker(onPick: (ImportDocument) -> Unit): (() -> Unit)? {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val callback = rememberUpdatedState(onPick)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            callback.value(ImportDocument(name = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull() ?: uri.lastPathSegment.orEmpty()) {
                withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } }
            })
        }
    }
    return remember(launcher) { { launcher.launch(arrayOf("*/*")) } }
}
