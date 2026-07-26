package com.garfiec.librechat.feature.files.platform

import com.garfiec.librechat.core.network.upload.StreamingUploadSource

/**
 * Platform-abstracted file reader.
 * Android: ContentResolver + Uri
 * iOS: NSFileManager / PHAsset
 */
interface FileReader {
    /**
     * Creates a reopenable streaming source from a platform-specific file reference.
     * @param fileRef Opaque platform reference (Android Uri, iOS URL)
     * @return streaming source, or null if the reference is invalid
     */
    fun openUploadSource(fileRef: Any): StreamingUploadSource?

    /**
     * Resolves the display filename from a platform file reference.
     */
    fun getFileName(fileRef: Any): String?

    /**
     * Resolves the MIME type from a platform file reference.
     */
    fun getMimeType(fileRef: Any): String?
}
