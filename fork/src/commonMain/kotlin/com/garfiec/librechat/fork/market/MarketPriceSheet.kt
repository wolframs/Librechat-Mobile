package com.garfiec.librechat.fork.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garfiec.librechat.core.ui.components.AdaptiveButton
import com.garfiec.librechat.core.ui.components.AdaptiveDivider
import com.garfiec.librechat.core.ui.components.AdaptiveModalBottomSheet
import com.garfiec.librechat.core.ui.components.AdaptiveTextButton
import com.garfiec.librechat.core.ui.components.share.LocalExternalUriHandler
import com.garfiec.librechat.fork.icons.ForkIcons
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketPriceSheet(server: String, model: String, onDismiss: () -> Unit) {
    AdaptiveModalBottomSheet(onDismissRequest = onDismiss,
        dragHandle = { com.garfiec.librechat.fork.composer.ComposerDragHandle() }) {
        MarketPriceContent(server, model, onDismiss = onDismiss)
    }
}

@Composable
fun MarketPriceContent(server: String, model: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val state = koinInject<MarketPriceState>()
    var attempt by remember { mutableIntStateOf(0) }
    val browser = LocalExternalUriHandler.current ?: LocalUriHandler.current
    LaunchedEffect(model, attempt) { state.load(model) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        com.garfiec.librechat.fork.composer.ComposerSheetHeader(
            stringResource(Res.string.composer_prices), onNavigate = onDismiss)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(model, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(Res.string.market_units), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        }
    }
}

@Composable
private fun MarketQuote(quote: MarketPrice) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val unknown = stringResource(Res.string.market_unknown)
        // Says whose price leads: what this key's routing allows, or the cheapest offer when that is unknown.
        Text(
            stringResource(if (quote.effective != null) Res.string.market_headline_key else Res.string.market_headline_best),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = muted(),
        )
        PriceLane.entries.forEach { lane ->
            val price = quote.price(lane)
            if (lane != PriceLane.REASONING || price != null) {
                val label = stringResource(
                    when (lane) {
                        PriceLane.INPUT -> Res.string.market_input
                        PriceLane.OUTPUT -> Res.string.market_output
                        PriceLane.CACHE_READ -> Res.string.market_cache_read
                        PriceLane.CACHE_WRITE -> Res.string.market_cache_write
                        PriceLane.REASONING -> Res.string.market_reasoning
                    },
                )
                val estimated = quote.effective?.cacheEstimated == true &&
                    lane in listOf(PriceLane.CACHE_READ, PriceLane.CACHE_WRITE)
                val value = (if (estimated) "≈" else "") + marketUsd(price, unknown)
                val list = quote.listPrice(lane)
                if (lane == PriceLane.INPUT || lane == PriceLane.OUTPUT) {
                    val against = list?.let {
                        stringResource(Res.string.market_against_list, marketUsd(it, unknown), marketDiscount(price, it, unknown))
                    }
                    HeadlinePrice(label, value, against)
                } else {
                    FigureRow(label, value, small = true)
                }
            }
        }
        if (quote.routing?.trustedOnly == true) {
            Text(
                stringResource(Res.string.market_routing, quote.hiddenSellers?.toString() ?: unknown),
                fontSize = 13.sp,
                color = muted(),
            )
        }
        val sellers = quote.sellers.orEmpty()
        if (sellers.isNotEmpty()) {
            Disclosure(stringResource(Res.string.market_sellers_group), sellers.size.toString()) {
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
        }
        val stats = buildList {
            quote.stats?.ttftP50Ms?.let { add(Res.string.market_ttft_label to "${marketNumber(it)} ms") }
            quote.stats?.tpsP50?.let { add(Res.string.market_tps_label to marketNumber(it)) }
            quote.stats?.uptimePct?.let { add(Res.string.market_uptime_label to "${marketNumber(it)}%") }
            quote.stats?.cacheHitRatePct?.let { add(Res.string.market_hits_label to "${marketNumber(it)}%") }
            (quote.stats?.requests24h ?: quote.requests24h)?.let { add(Res.string.market_requests_label to it.toString()) }
        }
        if (stats.isNotEmpty()) {
            Disclosure(stringResource(Res.string.market_performance), null) {
                stats.forEach { (label, value) -> FigureRow(stringResource(label), value, small = false) }
            }
        }
        Text(stringResource(Res.string.market_quote_note), fontSize = 13.sp, color = muted())
    }
}

/** Input or output: the price this key gets, large, with the list price and the discount beneath. */
@Composable
private fun HeadlinePrice(label: String, value: String, against: String?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 16.sp)
        Column(horizontalAlignment = Alignment.End) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"))
            if (against != null) Text(against, fontSize = 13.sp, color = muted())
        }
    }
}

@Composable
private fun FigureRow(label: String, value: String, small: Boolean) {
    val size = if (small) 13.sp else 16.sp
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), fontSize = size, color = if (small) muted() else MaterialTheme.colorScheme.onSurface)
        Text(value, fontSize = size, textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"))
    }
}

/** A 48 dp row that opens the detail beneath it; closed at first, so the prices lead the sheet. */
@Composable
private fun Disclosure(title: String, count: String?, content: @Composable () -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdaptiveDivider()
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), fontSize = 16.sp)
            if (count != null) Text(count, fontSize = 13.sp, color = muted(), modifier = Modifier.padding(end = 8.dp))
            Icon(if (open) ForkIcons.ChevronDown else ForkIcons.ChevronRight, null, Modifier.size(20.dp), tint = muted())
        }
        if (open) content()
    }
}

@Composable
private fun muted() = MaterialTheme.colorScheme.onSurfaceVariant

/** One row of four cells: a name, then three right-aligned figures. */
@Composable
private fun TableRow(name: String, first: String, second: String, third: String, header: Boolean = false) {
    val style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal,
        fontFeatureSettings = "tnum")
    val color = if (header) muted() else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(name, Modifier.weight(NAME_WEIGHT), style = style, color = color)
        listOf(first, second, third).forEach { cell ->
            Text(cell, Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End)
        }
    }
}

private const val NAME_WEIGHT = 1.5f
