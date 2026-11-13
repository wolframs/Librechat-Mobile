package com.garfiec.librechat.fork.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.common.identity.AccountState.Resolved
import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.Message
import com.garfiec.librechat.fork.cachettl.CacheTtlRail
import com.garfiec.librechat.fork.cachettl.CacheTtlSession
import com.garfiec.librechat.fork.cachettl.cacheTtlRoute
import com.garfiec.librechat.fork.resources.Res
import com.garfiec.librechat.fork.resources.market_costs
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun <T> ChatGutter(
    session: CacheTtlSession,
    endpoint: String,
    model: String?,
    server: String,
    configs: Map<String, EndpointConfig>,
    parameters: Map<String, String>,
    agentProvider: String?,
    agentModel: String?,
    agentParameters: Map<String, String>,
    state: StateFlow<T>,
    path: (T) -> List<Message>,
    modifier: Modifier = Modifier,
) {
    val accounts = koinInject<ActiveAccountProvider>()
    val account by accounts.state.collectAsState()
    val api = koinInject<MarketApi>()
    val route = cacheTtlRoute(endpoint, model, configs, parameters, agentProvider, agentModel, agentParameters)
    val target = if (endpoint == "agents") agentModel else model
    var marketplaces by remember(account, server) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(account, server) {
        if ((account as? Resolved)?.id != null) marketplaces = api.endpoints()
    }
    val available = route.endpoint in marketplaces && !target.isNullOrBlank()
    var open by remember(account, server, target, route.endpoint) { mutableStateOf(false) }
    val title = stringResource(Res.string.market_costs)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CacheTtlRail(session, endpoint, model, configs, parameters, agentProvider, agentModel,
            agentParameters, state, path, marketplace = route.endpoint in marketplaces)
        if (available) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(role = Role.Button) { open = true }.semantics { contentDescription = title },
                contentAlignment = Alignment.Center,
            ) { Text("$", style = MaterialTheme.typography.labelMedium) }
        }
    }
    if (open && available) {
        key(account, server, target, route.endpoint) {
            MarketPriceSheet(server, target.orEmpty(), onDismiss = { open = false })
        }
    }
}
