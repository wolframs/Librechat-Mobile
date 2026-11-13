package com.garfiec.librechat.fork.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.ui.components.AdaptiveButton
import com.garfiec.librechat.core.ui.components.AdaptiveDivider
import com.garfiec.librechat.core.ui.components.AdaptiveModalBottomSheet
import com.garfiec.librechat.core.ui.components.AdaptiveTextButton
import com.garfiec.librechat.core.ui.components.share.LocalExternalUriHandler
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketPriceSheet(server: String, model: String, onDismiss: () -> Unit) {
    val state = koinInject<MarketPriceState>()
    var attempt by remember { mutableIntStateOf(0) }
    val browser = LocalExternalUriHandler.current ?: LocalUriHandler.current
    LaunchedEffect(model, attempt) { state.load(model) }
    AdaptiveModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.market_costs), style = MaterialTheme.typography.titleLarge)
            Text(model, style = MaterialTheme.typography.bodySmall)
            when {
                state.loading -> Text(stringResource(Res.string.market_loading))
                state.price == null -> {
                    Text(stringResource(Res.string.market_unavailable))
                    AdaptiveTextButton(onClick = { attempt++ }) { Text(stringResource(Res.string.market_retry)) }
                }
                else -> MarketQuote(state.price!!)
            }
            AdaptiveDivider()
            Text(stringResource(Res.string.market_dashboard_note), style = MaterialTheme.typography.bodySmall)
            AdaptiveButton(onClick = { browser.openUri(server.trimEnd('/') + "/cost") }) {
                Text(stringResource(Res.string.market_dashboard))
            }
            AdaptiveTextButton(onClick = onDismiss) { Text(stringResource(Res.string.market_close)) }
        }
    }
}

@Composable
private fun MarketQuote(quote: MarketPrice) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val unknown = stringResource(Res.string.market_unknown)
        Text(stringResource(Res.string.market_units), style = MaterialTheme.typography.bodySmall)
        PriceLane.entries.forEach { lane ->
            val price = quote.price(lane)
            if (lane != PriceLane.REASONING || price != null) {
                val label = when (lane) {
                    PriceLane.INPUT -> Res.string.market_input
                    PriceLane.OUTPUT -> Res.string.market_output
                    PriceLane.CACHE_READ -> Res.string.market_cache_read
                    PriceLane.CACHE_WRITE -> Res.string.market_cache_write
                    PriceLane.REASONING -> Res.string.market_reasoning
                }
                val estimated = quote.effective?.cacheEstimated == true &&
                    lane in listOf(PriceLane.CACHE_READ, PriceLane.CACHE_WRITE)
                Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(Res.string.market_lane, (if (estimated) "≈" else "") + marketUsd(price, unknown),
                    marketUsd(quote.listPrice(lane), unknown), marketDiscount(price, quote.listPrice(lane), unknown)))
            }
        }
        if (quote.routing?.trustedOnly == true) {
            Text(stringResource(Res.string.market_routing, quote.hiddenSellers?.toString() ?: unknown))
        }
        AdaptiveDivider()
        Text(stringResource(Res.string.market_sellers), style = MaterialTheme.typography.titleMedium)
        quote.sellers.orEmpty().forEach { seller ->
            val trust = stringResource(when (seller.trusted) {
                true -> Res.string.market_trusted
                false -> Res.string.market_untrusted
                null -> Res.string.market_unknown
            })
            Text(seller.provider ?: unknown, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.market_seller, trust, seller.healthy?.toString() ?: unknown,
                marketUsd(seller.input, unknown), marketUsd(seller.output, unknown)))
        }
        quote.stats?.let { stats ->
            stats.ttftP50Ms?.let { Text(stringResource(Res.string.market_ttft, marketNumber(it))) }
            stats.tpsP50?.let { Text(stringResource(Res.string.market_tps, marketNumber(it))) }
            stats.uptimePct?.let { Text(stringResource(Res.string.market_uptime, marketNumber(it))) }
            stats.cacheHitRatePct?.let { Text(stringResource(Res.string.market_hits, marketNumber(it))) }
        }
        (quote.stats?.requests24h ?: quote.requests24h)?.let {
            Text(stringResource(Res.string.market_requests, it.toString()))
        }
        Text(stringResource(Res.string.market_quote_note), style = MaterialTheme.typography.bodySmall)
    }
}
