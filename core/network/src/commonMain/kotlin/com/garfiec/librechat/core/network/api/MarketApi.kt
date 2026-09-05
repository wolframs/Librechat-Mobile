package com.garfiec.librechat.core.network.api

import com.garfiec.librechat.core.model.MarketPrice
import com.garfiec.librechat.core.model.MarketplaceEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.path

class MarketApi(private val client: HttpClient) {
    suspend fun endpoints(): MarketplaceEndpoints = client.get {
        url { path("cost/markets/endpoints") }
    }.body()

    suspend fun price(model: String): MarketPrice = client.get {
        url { path("cost/markets") }
        parameter("model", model)
    }.body()
}
