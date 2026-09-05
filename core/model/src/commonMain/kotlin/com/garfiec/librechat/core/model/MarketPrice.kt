package com.garfiec.librechat.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceEndpoints(val endpoints: List<String>)

@Serializable
data class MarketPrice(
    val model: String,
    val best: MarketRates,
    val direct: MarketReference,
    val healthySellers: Int = 0,
    val fetchedAt: String? = null,
)

@Serializable
data class MarketRates(
    val input: Double? = null,
    val output: Double? = null,
    val cacheRead: Double? = null,
    val cacheWrite: Double? = null,
)

@Serializable
data class MarketReference(
    val input: Double? = null,
    val output: Double? = null,
    val source: String? = null,
)
