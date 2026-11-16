package com.garfiec.librechat.fork.market

import com.garfiec.librechat.core.common.result.apiCallCatching
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
private data class MarketplaceEndpoints(val endpoints: List<String> = emptyList())

/** Uses the ordinary authenticated client, including the deployment's base path. */
class MarketApi(private val client: HttpClient) {
    suspend fun price(model: String): MarketPrice? = apiCallCatching({
        client.get {
            url { path("cost/markets") }
            parameter("model", model)
        }.body<MarketPrice>()
    }, { null })

    suspend fun overview(): MarketOverview? = apiCallCatching({
        client.get { url { path("cost/overview") } }.body<MarketOverview>()
    }, { null })

    suspend fun routing(): MarketRoutingState? = apiCallCatching({
        client.get { url { path("cost/routing") } }.body<MarketRoutingState>()
    }, { null })

    suspend fun setTrust(choice: TrustedChoice): MarketRoutingState? = apiCallCatching({
        client.put {
            url { path("cost/routing") }
            contentType(ContentType.Application.Json)
            // Inherit must send an explicit null, despite the client's explicitNulls=false.
            setBody(buildJsonObject {
                put("allowUntrusted", choice.allowUntrusted?.let { JsonPrimitive(it) } ?: JsonNull)
            })
        }.body<MarketRoutingState>()
    }, { null })

    suspend fun endpoints(): Set<String> = apiCallCatching({
        client.get { url { path("cost/markets/endpoints") } }.body<MarketplaceEndpoints>().endpoints.toSet()
    }, { emptySet() })
}
