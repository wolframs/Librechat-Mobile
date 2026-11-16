package com.garfiec.librechat.fork.market

import kotlinx.serialization.Serializable

@Serializable
data class MarketOverview(val totals: SpendTotals? = null, val account: SpendAccount? = null) {
    val visible: Boolean
        get() = totals?.today != null || totals?.month != null ||
            (account?.configured == true && account.creditBalance != null)
}

@Serializable
data class SpendTotals(val today: Double? = null, val month: Double? = null)

@Serializable
data class SpendAccount(val configured: Boolean = false, val creditBalance: Double? = null)

@Serializable
data class MarketRoutingState(
    val configured: Boolean = false,
    val preferences: TrustPreference? = null,
    val keyPreferences: TrustPreference? = null,
    val accountPreferences: TrustPreference? = null,
    val writable: Boolean? = null,
) {
    val choice: TrustedChoice get() = trustedChoice(keyPreferences?.allowUntrusted)
    // Null means the sidecar has not observed a refusal; the web client also offers a write.
    val canWrite: Boolean get() = configured && writable != false
    val source: TrustSource get() = when {
        keyPreferences?.allowUntrusted != null -> TrustSource.KEY
        accountPreferences?.allowUntrusted != null -> TrustSource.ACCOUNT
        else -> TrustSource.DEFAULT
    }
}

@Serializable
data class TrustPreference(val allowUntrusted: Boolean? = null)

enum class TrustedChoice(val allowUntrusted: Boolean?) { ON(false), OFF(true), INHERIT(null) }
enum class TrustSource { KEY, ACCOUNT, DEFAULT }

fun trustedChoice(allowUntrusted: Boolean?): TrustedChoice =
    TrustedChoice.entries.first { it.allowUntrusted == allowUntrusted }

/** A denied or failed fetch is null, so it adds no surface to ordinary Prices. */
fun showSpend(overview: MarketOverview?): Boolean = overview?.visible == true
fun showTrust(routing: MarketRoutingState?): Boolean = routing?.configured == true
