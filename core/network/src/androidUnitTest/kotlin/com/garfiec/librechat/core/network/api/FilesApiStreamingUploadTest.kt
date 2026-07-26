package com.garfiec.librechat.core.network.api

import com.garfiec.librechat.core.network.upload.StreamingUploadSource
import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import org.junit.Test

class FilesApiStreamingUploadTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private suspend fun renderBody(content: OutgoingContent): String = coroutineScope {
        val writable = content as OutgoingContent.WriteChannelContent
        val channel = writer(Dispatchers.Default) { writable.writeTo(channel) }.channel
        channel.readRemaining().readByteArray().decodeToString()
    }

    @Test
    fun `multipart consumes the upload channel lazily without a file byte array parameter`() = runTest {
        var body: String? = null
        var opened = 0
        val engine = MockEngine { request ->
            body = renderBody(request.body)
            respond(
                content = """
                    {
                      "file_id":"f1",
                      "filename":"notes.txt",
                      "filepath":"/files/notes.txt",
                      "type":"text/plain",
                      "bytes":7
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        val payload = "payload".encodeToByteArray()
        val source = StreamingUploadSource(payload.size.toLong()) {
            opened++
            ByteReadChannel(payload)
        }

        val result = FilesApi(client).uploadFile(
            source = source,
            filename = "notes.txt",
            type = "text/plain",
            fileId = "f1",
            endpoint = "agents",
        )

        assertThat(opened).isEqualTo(1)
        assertThat(body).contains("filename=\"notes.txt\"")
        assertThat(body).contains("text/plain")
        assertThat(body).contains("payload")
        assertThat(result.fileId).isEqualTo("f1")
    }
}
