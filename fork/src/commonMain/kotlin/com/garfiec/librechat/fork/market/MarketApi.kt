package com.garfiec.librechat.fork.market

import com.garfiec.librechat.core.common.result.apiCallCatching
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.path
import kotlinx.serialization.Serializable

@Serializable
private data class MarketplaceEndpoints(val endpoints: List<String> = emptyList())

/** Uses the ordinary authenticated client, including the deployment's base path. */
class MarketApi(private val client: HttpClient) {
    suspend fun endpoints(): Set<String> = apiCallCatching({
        client.get { url { path("cost/markets/endpoints") } }.body<MarketplaceEndpoints>().endpoints.toSet()
    }, { emptySet() })
}
