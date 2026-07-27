package com.garfiec.librechat.feature.files.platform

import com.garfiec.librechat.core.network.upload.StreamingUploadSource
import io.ktor.utils.io.ByteReadChannel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSURL
import platform.Foundation.lastPathComponent
import platform.Foundation.pathExtension

@OptIn(ExperimentalForeignApi::class)
class IosFileReader : FileReader {

    override fun openUploadSource(fileRef: Any): StreamingUploadSource? {
        val url = fileRef as? NSURL ?: return null
        val path = url.path?.let(::Path) ?: return null
        val size = runCatching {
            SystemFileSystem.metadataOrNull(path)?.size?.takeIf { it >= 0L }
        }.getOrNull()
        return StreamingUploadSource(size) {
            val accessing = url.startAccessingSecurityScopedResource()
            try {
                val source = SystemFileSystem.source(path)
                ByteReadChannel(
                    SecurityScopedSource(
                        delegate = source,
                        url = url,
                        stopAccessingOnClose = accessing,
                    ).buffered(),
                )
            } catch (exception: Exception) {
                if (accessing) url.stopAccessingSecurityScopedResource()
                throw exception
            }
        }
    }

    override fun getFileName(fileRef: Any): String? {
        val url = fileRef as? NSURL ?: return null
        return url.lastPathComponent
    }

    override fun getMimeType(fileRef: Any): String? {
        val url = fileRef as? NSURL ?: return null
        val ext = url.pathExtension ?: return null
        return CommonMimeTypes.fromExtension(ext)
    }
}

/**
 * Balances the document provider's security scope with the streaming source lifecycle.
 *
 * Ktor closes the multipart source after success, failure, cancellation, and request replay. Each
 * replay calls [StreamingUploadSource.openChannel] again, so every channel owns an independent scope.
 */
@OptIn(ExperimentalForeignApi::class)
private class SecurityScopedSource(
    private val delegate: RawSource,
    private val url: NSURL,
    private val stopAccessingOnClose: Boolean,
) : RawSource {
    private var closed = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long =
        delegate.readAtMostTo(sink, byteCount)

    override fun close() {
        if (closed) return
        closed = true
        try {
            delegate.close()
        } finally {
            if (stopAccessingOnClose) url.stopAccessingSecurityScopedResource()
        }
    }
}
