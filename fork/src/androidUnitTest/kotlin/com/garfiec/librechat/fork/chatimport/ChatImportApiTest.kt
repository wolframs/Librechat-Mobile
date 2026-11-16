package com.garfiec.librechat.fork.chatimport

import com.garfiec.librechat.core.network.di.librechatJson
import com.garfiec.librechat.core.network.fork.chatimport.ChatImportAcknowledgement
import com.garfiec.librechat.core.network.fork.chatimport.ChatImportApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import org.junit.Assert.*
import org.junit.Test

class ChatImportApiTest {
    @Test fun multipartKeepsOriginalBytesAndIncludesTranscriptOptions() = runTest {
        val bytes = "## User:\n\ncafé\n".encodeToByteArray()
        val client = HttpClient(MockEngine { request ->
            assertEquals("/api/convos/import", request.url.encodedPath)
            val channel = ByteChannel(autoFlush = true)
            val writer = async { (request.body as MultiPartFormDataContent).writeTo(channel); channel.close() }
            val body = channel.readRemaining().readByteArray().decodeToString()
            writer.await()
            assertTrue(body.contains("name=\"file\""))
            assertTrue(body.contains("filename=\"chat%20export.MD\""))
            assertTrue(body.contains("Content-Type: text/markdown", ignoreCase = true))
            assertTrue(body.contains(bytes.decodeToString()))
            val parts = body.split("--" + (request.body as MultiPartFormDataContent).boundary)
            val fields = parts.filter { it.contains("Content-Disposition:", ignoreCase = true) }
            assertEquals(3, fields.size)
            assertEquals("5", fields.single { it.contains("name=\"maxImages\"") }.substringAfter("\r\n\r\n").trim())
            assertEquals("-60", fields.single { it.contains("name=\"timezoneOffset\"") }.substringAfter("\r\n\r\n").trim())
            respond("""{"message":"Conversation(s) imported successfully","conversationId":"imported-id"}""",
                HttpStatusCode.Created, headersOf("Content-Type", "application/json"))
        }) { install(ContentNegotiation) { json(librechatJson) } }
        try {
            val ack = ChatImportApi(client).upload(bytes, "chat export.MD", -60)
            assertEquals("imported-id", ack.conversationId)
        } finally { client.close() }
    }

    @Test fun acknowledgementAcceptsJsonBulkSuccessWithoutConversationId() {
        val ack = librechatJson.decodeFromString<ChatImportAcknowledgement>(
            """{"message":"Conversation(s) imported successfully"}""",
        )
        assertNull(ack.conversationId)
        assertEquals("Conversation(s) imported successfully", ack.message)
    }
}
