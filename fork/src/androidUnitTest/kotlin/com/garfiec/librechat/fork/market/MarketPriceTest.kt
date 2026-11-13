package com.garfiec.librechat.fork.market

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketPriceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun minimalResponseKeepsMissingPricesUnknown() {
        val quote = json.decodeFromString<MarketPrice>("""{"model":"old","best":{"input":null},"direct":{}}""")
        assertNull(quote.price(PriceLane.INPUT))
        assertNull(quote.routing)
        assertNull(quote.stats)
        assertEquals("unknown", marketUsd(quote.price(PriceLane.CACHE_READ), "unknown"))
    }

    @Test fun fullResponseDecodesOptionalSidecarFields() {
        val quote = json.decodeFromString<MarketPrice>("""{
            "model":"model/id", "best":{"input":1,"output":2,"cacheRead":0.1,"cacheWrite":1.25,"reasoning":3},
            "direct":{"input":5,"output":10,"source":"provider","marketplaceInput":4,"marketplaceOutput":8},
            "discountPct":80,"trend":{"direction":"up","currentPct":80,"previousPct":70,"buckets":[null,70,80]},
            "sellers":[{"provider":"family","trusted":true,"healthy":2,"input":1,"output":2}],
            "healthySellers":2,"requests24h":42,
            "routing":{"trustedOnly":true,"source":"key","trustSource":"offers"},
            "effective":{"input":2,"output":3,"cacheRead":0.2,"cacheWrite":2.5,"reasoning":4,
                "cacheEstimated":true,"discountPct":60,"provider":"family"},"hiddenSellers":3,
            "stats":{"ttftP50Ms":500,"tpsP50":40,"uptimePct":99.9,"cacheHitRatePct":80,
                "medianDiscountPct":70,"bestTrustedDiscountPct":80,"requests24h":40,"requests7d":200,"realized7dPct":65}
        }""")
        assertEquals(2.0, quote.price(PriceLane.INPUT))
        assertEquals(4.0, quote.price(PriceLane.REASONING))
        assertEquals(2, quote.sellers!!.single().healthy)
        assertTrue(quote.routing!!.trustedOnly!!)
        assertEquals(40L, quote.stats!!.requests24h)
        assertEquals(listOf(null, 70.0, 80.0), quote.trend!!.buckets)
    }

    @Test fun effectiveFallsBackPerLaneAndPreservesZero() {
        val quote = MarketPrice("m", MarketRates(input = 1.0, output = 2.0), MarketReference(),
            effective = MarketRates(input = 0.0))
        assertEquals(0.0, quote.price(PriceLane.INPUT))
        assertEquals(2.0, quote.price(PriceLane.OUTPUT))
        assertNull(quote.listPrice(PriceLane.CACHE_READ))
    }

    @Test fun dollarsAndDiscountsDoNotInventZeroPrices() {
        assertEquals("\$1.2346", marketUsd(1.234567, "unknown"))
        assertEquals("\$0", marketUsd(0.0, "unknown"))
        assertEquals("<\$0.0001", marketUsd(0.000001, "unknown"))
        assertEquals("unknown", marketUsd(Double.NaN, "unknown"))
        assertEquals("unknown", marketUsd(-1.0, "unknown"))
        assertEquals("75%", marketDiscount(1.0, 4.0, "unknown"))
        assertEquals("-25%", marketDiscount(5.0, 4.0, "unknown"))
        assertEquals("unknown", marketDiscount(null, 4.0, "unknown"))
        assertEquals("unknown", marketDiscount(1.0, 0.0, "unknown"))
    }
}
