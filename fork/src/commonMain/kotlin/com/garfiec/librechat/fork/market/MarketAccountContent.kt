package com.garfiec.librechat.fork.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.ui.components.AdaptiveDivider
import com.garfiec.librechat.core.ui.components.AdaptiveSegmentedChoice
import com.garfiec.librechat.fork.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MarketAccountContent(state: MarketPriceState, model: String) {
    state.overview?.takeIf { showSpend(it) }?.let { overview ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.market_spend), style = MaterialTheme.typography.titleSmall)
            val unknown = stringResource(Res.string.market_unknown)
            if (overview.account?.configured == true) {
                overview.account.creditBalance?.let { SpendFigure(stringResource(Res.string.market_credit), marketUsd(it, unknown)) }
            }
            overview.totals?.today?.let { SpendFigure(stringResource(Res.string.market_spend_today), marketUsd(it, unknown)) }
            overview.totals?.month?.let { SpendFigure(stringResource(Res.string.market_spend_month), marketUsd(it, unknown)) }
            Text(stringResource(Res.string.market_spend_scope), style = MaterialTheme.typography.bodySmall)
        }
        AdaptiveDivider()
    }
    state.routing?.takeIf { showTrust(it) }?.let { routing ->
        TrustControl(state, routing, model)
        AdaptiveDivider()
    }
}

@Composable
private fun SpendFigure(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
    }
}

@Composable
private fun TrustControl(state: MarketPriceState, routing: MarketRoutingState, model: String) {
    val scope = rememberCoroutineScope()
    val labels = listOf(stringResource(Res.string.market_trust_on), stringResource(Res.string.market_trust_off),
        stringResource(Res.string.market_trust_inherit))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.market_trust), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.market_trust_help), style = MaterialTheme.typography.bodySmall)
        AdaptiveSegmentedChoice(
            options = labels,
            selectedIndex = (state.requested ?: routing.choice).ordinal,
            onSelect = { index -> scope.launch { state.setTrust(TrustedChoice.entries[index], model) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = routing.canWrite && state.requested == null,
        )
        val effective = when (trustedChoice(routing.preferences?.allowUntrusted)) {
            TrustedChoice.ON -> labels[0]
            TrustedChoice.OFF -> labels[1]
            TrustedChoice.INHERIT -> stringResource(Res.string.market_trust_default)
        }
        val source = stringResource(when (routing.source) {
            TrustSource.KEY -> Res.string.market_trust_key
            TrustSource.ACCOUNT -> Res.string.market_trust_account
            TrustSource.DEFAULT -> Res.string.market_trust_default
        })
        Text(stringResource(Res.string.market_trust_effective, effective, source), style = MaterialTheme.typography.bodySmall)
        if (!routing.canWrite) Text(stringResource(Res.string.market_trust_readonly), style = MaterialTheme.typography.bodySmall)
        if (state.requested != null) Text(stringResource(Res.string.market_trust_saving), style = MaterialTheme.typography.bodySmall)
        if (state.saveFailed) Text(stringResource(Res.string.market_trust_failed),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}
