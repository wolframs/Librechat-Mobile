package com.garfiec.librechat.fork.market

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.garfiec.librechat.core.common.identity.AccountState.Resolved
import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.Message
import com.garfiec.librechat.fork.cachettl.CacheTtlRail
import com.garfiec.librechat.fork.cachettl.CacheTtlSession
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

@Composable
fun <T> ChatGutter(
    session: CacheTtlSession, endpoint: String, model: String?, server: String,
    configs: Map<String, EndpointConfig>, parameters: Map<String, String>,
    agentProvider: String?, agentModel: String?, agentParameters: Map<String, String>,
    state: StateFlow<T>, path: (T) -> List<Message>, modifier: Modifier = Modifier,
) {
    val accounts = koinInject<ActiveAccountProvider>()
    val account by accounts.state.collectAsState()
    val api = koinInject<MarketApi>()
    var markets by remember(account, server) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(account, server) { if ((account as? Resolved)?.id != null) markets = api.endpoints() }
    val route = com.garfiec.librechat.fork.cachettl.cacheTtlRoute(endpoint, model, configs, parameters,
        agentProvider, agentModel, agentParameters)
    CacheTtlRail(session, endpoint, model, configs, parameters, agentProvider, agentModel,
        agentParameters, state, path, modifier, marketplace = route.endpoint in markets)
}
