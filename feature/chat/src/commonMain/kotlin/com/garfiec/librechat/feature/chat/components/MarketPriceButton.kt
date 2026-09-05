package com.garfiec.librechat.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garfiec.librechat.feature.chat.resources.*
import com.garfiec.librechat.feature.chat.resources.Res
import com.garfiec.librechat.feature.chat.viewmodel.MarketViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.round

@Composable
internal fun MarketPriceButton(serverUrl: String, endpoint: String, model: String?) {
    val viewModel = koinViewModel<MarketViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember(serverUrl) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(serverUrl) { viewModel.discover(serverUrl) }
    LaunchedEffect(expanded, endpoint, model) { if (expanded) viewModel.load(endpoint, model) }
    if (!state.available || state.serverUrl != serverUrl) return
    FloatingBarIconButton(
        icon = Icons.Default.Payments,
        contentDescription = stringResource(Res.string.market_costs),
        onClick = { expanded = true },
    )
    Spacer(Modifier.width(8.dp))
    if (!expanded) return
    val price = state.price.takeIf { state.selection == endpoint to model }
    AlertDialog(
        onDismissRequest = { expanded = false },
        title = { Text(stringResource(Res.string.market_costs)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (endpoint in state.endpoints) {
                    Text(model.orEmpty(), style = MaterialTheme.typography.titleSmall)
                    if (state.loading) CircularProgressIndicator()
                    if (state.failed) {
                        Text(stringResource(Res.string.market_unavailable))
                        TextButton(onClick = { viewModel.load(endpoint, model) }) {
                            Text(stringResource(Res.string.market_refresh))
                        }
                    }
                    price?.let {
                        Text(stringResource(Res.string.market_best, marketUsd(it.best.input), marketUsd(it.best.output)))
                        Text(stringResource(Res.string.market_cache, marketUsd(it.best.cacheRead), marketUsd(it.best.cacheWrite)))
                        val reference = if (it.direct.source == "provider") {
                            Res.string.market_provider_list
                        } else {
                            Res.string.marketplace_list
                        }
                        Text(stringResource(reference, marketUsd(it.direct.input), marketUsd(it.direct.output)))
                        Text(stringResource(Res.string.market_sellers, it.healthySellers))
                        it.fetchedAt?.let { timestamp -> Text(stringResource(Res.string.market_updated, timestamp)) }
                        Text(stringResource(Res.string.market_quote_note), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.load(endpoint, model) }) {
                            Text(stringResource(Res.string.market_refresh))
                        }
                    }
                }
                Text(stringResource(Res.string.cost_dashboard_description))
                Text(stringResource(Res.string.cost_mcp_excluded), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { uriHandler.openUri(serverUrl.trimEnd('/') + "/cost") }) {
                Text(stringResource(Res.string.cost_open_dashboard))
            }
        },
        dismissButton = {
            TextButton(onClick = { expanded = false }) { Text(stringResource(Res.string.market_close)) }
        },
    )
}

/** Missing quotes stay visibly unknown; they must never look like a zero-dollar offer. */
internal fun marketUsd(value: Double?): String =
    if (value == null || !value.isFinite() || value < 0) "—" else "$${round(value * 10000) / 10000}"
