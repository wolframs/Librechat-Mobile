package com.garfiec.librechat.feature.files.platform

import com.garfiec.librechat.core.network.upload.StreamingUploadSource
import io.ktor.utils.io.ByteReadChannel
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSURL
import platform.Foundation.lastPathComponent
import platform.Foundation.pathExtension

class IosFileReader : FileReader {

    override fun openUploadSource(fileRef: Any): StreamingUploadSource? {
        val url = fileRef as? NSURL ?: return null
        val path = url.path?.let(::Path) ?: return null
        val size = runCatching {
            SystemFileSystem.metadataOrNull(path)?.size?.takeIf { it >= 0L }
        }.getOrNull()
        return StreamingUploadSource(size) {
            ByteReadChannel(SystemFileSystem.source(path).buffered())
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
