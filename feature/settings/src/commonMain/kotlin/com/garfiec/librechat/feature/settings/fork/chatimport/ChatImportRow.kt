package com.garfiec.librechat.feature.settings.fork.chatimport

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.ui.components.AdaptiveCircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garfiec.librechat.feature.settings.screen.SettingsRow
import com.garfiec.librechat.fork.chatimport.ImportStatus
import com.garfiec.librechat.fork.chatimport.ChatImportViewModel
import com.garfiec.librechat.fork.chatimport.rememberChatImportPicker
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatImportRow(snackbar: SnackbarHostState, viewModel: ChatImportViewModel = koinViewModel()) {
    val launch = rememberChatImportPicker(viewModel::importChat) ?: return
    val status by viewModel.status.collectAsStateWithLifecycle()
    val message = when (status) {
        ImportStatus.SUCCEEDED -> stringResource(Res.string.chat_import_success)
        ImportStatus.FAILED -> stringResource(Res.string.chat_import_failed)
        ImportStatus.INVALID_FILE -> stringResource(Res.string.chat_import_invalid)
        else -> null
    }
    LaunchedEffect(message) {
        if (message != null) {
            // Dismissing first would null the key and cancel this effect mid-snackbar.
            snackbar.showSnackbar(message)
            viewModel.dismissStatus()
        }
    }
    SettingsRow(
        title = stringResource(Res.string.chat_import_title),
        summary = stringResource(if (status == ImportStatus.IMPORTING) Res.string.chat_import_progress else Res.string.chat_import_summary),
        onClick = launch,
        enabled = status != ImportStatus.IMPORTING,
        trailingContent = if (status == ImportStatus.IMPORTING) {
            { AdaptiveCircularProgressIndicator(modifier = Modifier.size(24.dp)) }
        } else null,
    )
}
