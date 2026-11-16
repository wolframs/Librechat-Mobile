package com.garfiec.librechat.core.network.fork.chatimport

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import io.ktor.http.path
import kotlinx.serialization.Serializable

@Serializable
data class ChatImportAcknowledgement(val message: String, val conversationId: String? = null)

/** The import route parses the original document and returns an acknowledgement, not a chat. */
class ChatImportApi(private val client: HttpClient) {
    suspend fun upload(bytes: ByteArray, filename: String, timezoneOffset: Int): ChatImportAcknowledgement =
        client.submitFormWithBinaryData(formData = formData {
            append("file", bytes, Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=\"${filename.encodeURLParameter()}\"")
                append(HttpHeaders.ContentType, if (filename.endsWith(".json", true)) "application/json" else "text/markdown")
            })
            append("maxImages", "5")
            // JavaScript getTimezoneOffset is UTC minus local time, in minutes.
            append("timezoneOffset", timezoneOffset.toString())
        }) {
            url { path("api/convos/import") }
        }.body()
}
