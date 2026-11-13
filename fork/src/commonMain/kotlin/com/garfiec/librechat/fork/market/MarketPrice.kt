package com.garfiec.librechat.fork.market

import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
data class MarketPrice(
    val model: String,
    val best: MarketRates,
    val direct: MarketReference,
    val discountPct: Double? = null,
    val trend: MarketTrend? = null,
    val sellers: List<MarketSeller>? = null,
    val healthySellers: Int? = null,
    val requests24h: Long? = null,
    val routing: MarketRouting? = null,
    val effective: MarketRates? = null,
    val hiddenSellers: Int? = null,
    val stats: MarketStats? = null,
)

@Serializable
data class MarketRates(
    val input: Double? = null,
    val output: Double? = null,
    val cacheRead: Double? = null,
    val cacheWrite: Double? = null,
    val reasoning: Double? = null,
    val cacheEstimated: Boolean? = null,
    val discountPct: Double? = null,
    val provider: String? = null,
)

@Serializable
data class MarketReference(
    val input: Double? = null,
    val output: Double? = null,
    val source: String? = null,
    val marketplaceInput: Double? = null,
    val marketplaceOutput: Double? = null,
)

@Serializable
data class MarketRouting(
    val trustedOnly: Boolean? = null,
    val source: String? = null,
    val trustSource: String? = null,
)

@Serializable
data class MarketSeller(
    val provider: String? = null,
    val trusted: Boolean? = null,
    val healthy: Int? = null,
    val input: Double? = null,
    val output: Double? = null,
)

@Serializable
data class MarketTrend(
    val direction: String? = null,
    val currentPct: Double? = null,
    val previousPct: Double? = null,
    val buckets: List<Double?>? = null,
)

@Serializable
data class MarketStats(
    val ttftP50Ms: Double? = null,
    val tpsP50: Double? = null,
    val uptimePct: Double? = null,
    val cacheHitRatePct: Double? = null,
    val medianDiscountPct: Double? = null,
    val bestTrustedDiscountPct: Double? = null,
    val requests24h: Long? = null,
    val requests7d: Long? = null,
    val realized7dPct: Double? = null,
)

enum class PriceLane { INPUT, OUTPUT, CACHE_READ, CACHE_WRITE, REASONING }

fun MarketPrice.price(lane: PriceLane): Double? = effective?.value(lane) ?: best.value(lane)

private fun MarketRates.value(lane: PriceLane): Double? = when (lane) {
    PriceLane.INPUT -> input
    PriceLane.OUTPUT -> output
    PriceLane.CACHE_READ -> cacheRead
    PriceLane.CACHE_WRITE -> cacheWrite
    PriceLane.REASONING -> reasoning
}

fun MarketPrice.listPrice(lane: PriceLane): Double? = when (lane) {
    PriceLane.INPUT -> direct.input
    PriceLane.OUTPUT -> direct.output
    else -> null // The sidecar supplies no list price for cache or reasoning lanes.
}

fun marketNumber(value: Double): String = (round(value * 10000) / 10000).toString().removeSuffix(".0")

/** Missing and invalid quotes must not look like free offers. */
fun marketUsd(value: Double?, unknown: String): String = when {
    value == null || !value.isFinite() || value < 0 -> unknown
    value > 0 && value < 0.0001 -> "<\$0.0001"
    else -> "\$${marketNumber(value)}"
}

fun marketDiscount(price: Double?, list: Double?, unknown: String): String {
    if (price == null || list == null) return unknown
    if (!price.isFinite() || !list.isFinite() || price < 0 || list <= 0) return unknown
    return "${round((1 - price / list) * 100).toLong()}%"
}
