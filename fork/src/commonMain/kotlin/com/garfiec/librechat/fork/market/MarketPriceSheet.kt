package com.garfiec.librechat.fork.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val unknown = stringResource(Res.string.market_unknown)
        Text(stringResource(Res.string.market_units), style = MaterialTheme.typography.bodySmall, color = muted())
        TableRow(
            "",
            stringResource(Res.string.market_col_key),
            stringResource(Res.string.market_col_list),
            stringResource(Res.string.market_col_off),
            header = true,
        )
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
                val list = quote.listPrice(lane)
                TableRow(
                    stringResource(label),
                    (if (estimated) "≈" else "") + marketUsd(price, unknown),
                    if (list == null) "–" else marketUsd(list, unknown),
                    if (list == null) "–" else marketDiscount(price, list, unknown),
                )
            }
        }
        if (quote.routing?.trustedOnly == true) {
            Text(
                stringResource(Res.string.market_routing, quote.hiddenSellers?.toString() ?: unknown),
                style = MaterialTheme.typography.bodySmall,
                color = muted(),
            )
        }
        val sellers = quote.sellers.orEmpty()
        if (sellers.isNotEmpty()) {
            AdaptiveDivider(Modifier.padding(vertical = 6.dp))
            TableRow(
                stringResource(Res.string.market_sellers),
                stringResource(Res.string.market_col_offers),
                stringResource(Res.string.market_input),
                stringResource(Res.string.market_output),
                header = true,
            )
            sellers.forEach { seller ->
                val name = seller.provider ?: unknown
                TableRow(
                    if (seller.trusted == false) "$name (${stringResource(Res.string.market_untrusted)})" else name,
                    seller.healthy?.toString() ?: unknown,
                    marketUsd(seller.input, unknown),
                    marketUsd(seller.output, unknown),
                )
            }
        }
        val stats = buildList {
            quote.stats?.ttftP50Ms?.let { add(stringResource(Res.string.market_ttft, marketNumber(it))) }
            quote.stats?.tpsP50?.let { add(stringResource(Res.string.market_tps, marketNumber(it))) }
            quote.stats?.uptimePct?.let { add(stringResource(Res.string.market_uptime, marketNumber(it))) }
            quote.stats?.cacheHitRatePct?.let { add(stringResource(Res.string.market_hits, marketNumber(it))) }
            (quote.stats?.requests24h ?: quote.requests24h)?.let {
                add(stringResource(Res.string.market_requests, it.toString()))
            }
        }
        if (stats.isNotEmpty()) {
            AdaptiveDivider(Modifier.padding(vertical = 6.dp))
            Text(stats.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = muted())
        }
        Text(stringResource(Res.string.market_quote_note), style = MaterialTheme.typography.bodySmall, color = muted())
    }
}

@Composable
private fun muted() = MaterialTheme.colorScheme.onSurfaceVariant

/** One row of four cells: a name, then three right-aligned figures. */
@Composable
private fun TableRow(name: String, first: String, second: String, third: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) muted() else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(NAME_WEIGHT), style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        listOf(first, second, third).forEach { cell ->
            Text(cell, Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End, maxLines = 1)
        }
    }
}

private const val NAME_WEIGHT = 1.5f
