package com.garfiec.librechat.feature.files.platform

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import com.garfiec.librechat.core.network.upload.StreamingUploadSource
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.Dispatchers

class AndroidFileReader(
    private val context: Application,
) : FileReader {

    override fun openUploadSource(fileRef: Any): StreamingUploadSource? {
        val uri = fileRef as? Uri ?: return null
        val contentLength = queryLong(uri, OpenableColumns.SIZE)?.takeIf { it >= 0L }
        return StreamingUploadSource(contentLength) {
            val input = checkNotNull(context.contentResolver.openInputStream(uri)) {
                "Content provider could not reopen the selected file"
            }
            input.toByteReadChannel(context = Dispatchers.IO)
        }
    }

    override fun getFileName(fileRef: Any): String? {
        val uri = fileRef as? Uri ?: return null
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        return cursor.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else {
                null
            }
        }
    }

    override fun getMimeType(fileRef: Any): String? {
        val uri = fileRef as? Uri ?: return null
        return context.contentResolver.getType(uri)
            ?: uri.lastPathSegment
                ?.substringAfterLast('.', "")
                ?.takeIf { it.isNotEmpty() }
                ?.let { CommonMimeTypes.fromExtension(it) }
    }

    private fun queryLong(uri: Uri, column: String): Long? {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(column),
            null,
            null,
            null,
        ) ?: return null
        return cursor.use {
            if (!it.moveToFirst()) return@use null
            val index = it.getColumnIndex(column)
            if (index < 0 || it.isNull(index)) null else it.getLong(index)
        }
    }
}
