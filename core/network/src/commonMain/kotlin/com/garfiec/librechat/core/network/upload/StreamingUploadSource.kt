package com.garfiec.librechat.core.network.upload

import io.ktor.utils.io.ByteReadChannel

/**
 * Reopenable content for a multipart upload.
 *
 * [openChannel] must return a fresh channel on every call: Ktor may replay a request after
 * authentication refresh or a configured retry. The channel is consumed incrementally and is
 * cancelled with the request, so callers do not need to materialize the full file in memory.
 */
class StreamingUploadSource(
    val contentLength: Long?,
    val openChannel: () -> ByteReadChannel,
)
