package com.garfiec.librechat.fork.market

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Owned by one sheet opening; leaving the sheet cancels its fetch and drops its quote. */
class MarketPriceState(private val api: MarketApi) {
    var loading by mutableStateOf(true)
        private set
    var price by mutableStateOf<MarketPrice?>(null)
        private set

    suspend fun load(model: String) {
        loading = true
        price = null
        price = api.price(model)
        loading = false
    }
}
