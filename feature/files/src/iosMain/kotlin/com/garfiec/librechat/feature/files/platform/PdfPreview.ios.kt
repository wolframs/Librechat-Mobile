package com.garfiec.librechat.feature.files.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import co.touchlab.kermit.Logger
import com.garfiec.librechat.core.ui.media.toNSData
import com.garfiec.librechat.feature.files.FilePreviewDisplayData
import com.garfiec.librechat.feature.files.resources.*
import com.garfiec.librechat.feature.files.resources.Res
import com.garfiec.librechat.feature.files.screen.InfoRow
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView

@OptIn(ExperimentalForeignApi::class)
private sealed interface IosPdfLoadState {
    data object Loading : IosPdfLoadState
    data class Success(val document: PDFDocument) : IosPdfLoadState
    data class Error(val message: StringResource, val detail: String? = null) : IosPdfLoadState
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PdfPreview(
    file: FilePreviewDisplayData,
    onDownloadFile: (suspend (fileId: String, userId: String?) -> ByteArray?)?,
    modifier: Modifier,
) {
    val currentDownloader by rememberUpdatedState(onDownloadFile)
    val loadState by produceState<IosPdfLoadState>(
        IosPdfLoadState.Loading,
        file.fileId,
        file.userId,
    ) {
        value = IosPdfLoadState.Loading
        val bytes = try {
            currentDownloader?.invoke(file.fileId, file.userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Logger.e(exception) { "PdfPreview: iOS download failed" }
            value = IosPdfLoadState.Error(Res.string.failed_to_download_pdf, exception.message)
            return@produceState
        }
        if (bytes == null) {
            value = IosPdfLoadState.Error(Res.string.failed_to_download_pdf)
            return@produceState
        }
        val document = runCatching { PDFDocument(bytes.toNSData()) }.getOrNull()
            ?.takeIf { !it.isLocked() && it.pageCount() > 0uL }
        value = document?.let(IosPdfLoadState::Success)
            ?: IosPdfLoadState.Error(Res.string.failed_to_render_pdf)
    }

    when (val state = loadState) {
        IosPdfLoadState.Loading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(Res.string.loading_pdf),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        is IosPdfLoadState.Error -> {
            IosPdfErrorFallback(file, state.message, state.detail, modifier)
        }
        is IosPdfLoadState.Success -> {
            UIKitView(
                modifier = modifier.fillMaxSize(),
                factory = {
                    PDFView().apply {
                        setAutoScales(true)
                        setDocument(state.document)
                    }
                },
                update = { view ->
                    if (view.document != state.document) view.setDocument(state.document)
                },
            )
        }
    }
}

@Composable
private fun IosPdfErrorFallback(
    file: FilePreviewDisplayData,
    message: StringResource,
    detail: String?,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = file.filename,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.could_not_render_pdf),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(message),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                detail?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(stringResource(Res.string.info_type), file.type)
                    InfoRow(stringResource(Res.string.info_size), file.formattedSize)
                    file.createdAt?.let { InfoRow(stringResource(Res.string.info_created), it) }
                    file.source?.let { InfoRow(stringResource(Res.string.info_source), it) }
                }
            }
        }
    }
}
