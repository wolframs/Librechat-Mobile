package com.garfiec.librechat.fork.market

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketAccountTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun overviewDecodesCreditAndActualWindowsWithoutInventingMissingFigures() {
        val overview = json.decodeFromString<MarketOverview>("""{
            "totals":{"today":0,"week":4,"month":12.34,"all":99},
            "account":{"configured":true,"creditBalance":23.45,"spent":9},"tools":{}
        }""")
        assertEquals(0.0, overview.totals?.today)
        assertEquals(12.34, overview.totals?.month)
        assertEquals(23.45, overview.account?.creditBalance)
        assertTrue(showSpend(overview))
        val missing = json.decodeFromString<MarketOverview>("""{"account":{"configured":false}}""")
        assertNull(missing.totals)
        assertFalse(showSpend(missing))
    }

    @Test fun routingDecodesKeyChoiceAndInheritedEffectiveValue() {
        val routing = json.decodeFromString<MarketRoutingState>("""{
            "configured":true,"key":{"id":"test"},"keyPreferences":{"allowUntrusted":null},
            "accountPreferences":{"allowUntrusted":false},"preferences":{"allowUntrusted":false},
            "writable":null,"providers":[]
        }""")
        assertEquals(TrustedChoice.INHERIT, routing.choice)
        assertEquals(TrustSource.ACCOUNT, routing.source)
        assertEquals(TrustedChoice.ON, trustedChoice(routing.preferences?.allowUntrusted))
        assertTrue(routing.canWrite)
        assertEquals(false, TrustedChoice.ON.allowUntrusted)
        assertEquals(true, TrustedChoice.OFF.allowUntrusted)
        assertNull(TrustedChoice.INHERIT.allowUntrusted)
    }

    @Test fun deniedResponsesAddNothingAndCapabilitiesGateWrites() {
        // Failed/forbidden API reads produce null rather than an empty or error card.
        assertFalse(showSpend(null))
        assertFalse(showTrust(null))
        val unconfigured = json.decodeFromString<MarketRoutingState>("""{"configured":false}""")
        assertFalse(showTrust(unconfigured))
        assertFalse(unconfigured.canWrite)
        val restricted = json.decodeFromString<MarketRoutingState>("""{
            "configured":true,"writable":false,"keyPreferences":{"allowUntrusted":false},
            "preferences":{"allowUntrusted":false},"writeBlockedReason":"restricted_key"
        }""")
        assertTrue(showTrust(restricted))
        assertFalse(restricted.canWrite)
        assertEquals(TrustedChoice.ON, restricted.choice)
        assertEquals(TrustSource.KEY, restricted.source)
        val permitted = json.decodeFromString<MarketRoutingState>("""{"configured":true}""")
        assertTrue(permitted.canWrite)
        assertEquals(TrustSource.DEFAULT, permitted.source)
    }
}
