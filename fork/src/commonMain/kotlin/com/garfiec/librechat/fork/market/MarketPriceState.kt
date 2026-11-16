package com.garfiec.librechat.fork.market

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Owned by one sheet opening; leaving the sheet cancels its fetch and drops its quote. */
class MarketPriceState(private val api: MarketApi) {
    var loading by mutableStateOf(true)
        private set
    var price by mutableStateOf<MarketPrice?>(null)
        private set

    var overview by mutableStateOf<MarketOverview?>(null)
        private set
    var routing by mutableStateOf<MarketRoutingState?>(null)
        private set
    var requested by mutableStateOf<TrustedChoice?>(null)
        private set
    var saveFailed by mutableStateOf(false)
        private set

    suspend fun loadAccount() = coroutineScope {
        val spend = async { api.overview() }
        val trust = async { api.routing() }
        overview = spend.await()
        routing = trust.await()
    }

    suspend fun setTrust(choice: TrustedChoice, model: String) {
        if (requested != null || routing?.canWrite != true) return
        val previous = routing
        requested = choice
        saveFailed = false
        try {
            val saved = api.setTrust(choice)
            saveFailed = saved == null
            routing = saved ?: api.routing() ?: previous
        } finally {
            requested = null
        }
        // A timed-out write can still have changed the key's eligible sellers.
        load(model)
    }

    suspend fun load(model: String) {
        loading = true
        price = null
        price = api.price(model)
        loading = false
    }
}
