package com.garfiec.librechat.fork.cachettl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.garfiec.librechat.core.common.identity.AccountState.Resolved
import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.data.fork.cachettl.CacheTtlMarkerStore
import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.Message
import com.garfiec.librechat.core.ui.components.AdaptiveAssistChip
import com.garfiec.librechat.core.ui.components.AdaptiveTextButton
import com.garfiec.librechat.fork.market.MarketApi
import com.garfiec.librechat.fork.resources.Res
import com.garfiec.librechat.fork.resources.cache_ttl_armed
import com.garfiec.librechat.fork.resources.cache_ttl_expired
import com.garfiec.librechat.fork.resources.cache_ttl_help
import com.garfiec.librechat.fork.resources.cache_ttl_info
import com.garfiec.librechat.fork.resources.cache_ttl_market
import com.garfiec.librechat.fork.resources.cache_ttl_none
import com.garfiec.librechat.fork.resources.cache_ttl_readonly
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock

/** The hot message path is collected here, so the surrounding chrome stays neutralized. */
@Composable
fun <T> CacheTtlRail(
    session: CacheTtlSession,
    endpoint: String,
    model: String?,
    configs: Map<String, EndpointConfig>,
    parameters: Map<String, String>,
    agentProvider: String?,
    agentModel: String?,
    agentParameters: Map<String, String>,
    state: StateFlow<T>,
    path: (T) -> List<Message>,
    modifier: Modifier = Modifier,
) {
    val route = cacheTtlRoute(endpoint, model, configs, parameters, agentProvider, agentModel, agentParameters)
    LaunchedEffect(route.canExtend) { session.reconcile(route.canExtend) }
    if (!route.shown) return
    val store = koinInject<CacheTtlMarkerStore>()
    val accounts = koinInject<ActiveAccountProvider>()
    val market = koinInject<MarketApi>()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val markers by remember(store, accounts) { store.observe(accounts) }.collectAsState(emptyList())
    val currentPath by remember(state) { state.map(path).distinctUntilChanged() }.collectAsState(path(state.value))
    val account by accounts.state.collectAsState()
    var marketplaces by remember(account) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(account) {
        marketplaces = if ((account as? Resolved)?.id != null) market.endpoints() else emptySet()
    }
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { now = Clock.System.now().toEpochMilliseconds(); delay(1000) }
        }
    }
    val anchor = cacheTtlAnchor(currentPath, markers)
    val remaining = anchor?.let { remainingMillis(it, now) }
    val countdown = when {
        remaining == null -> stringResource(Res.string.cache_ttl_none)
        remaining <= 0 -> stringResource(Res.string.cache_ttl_expired)
        else -> formatRemaining(remaining)
    }
    val arm = session.arm.takeIf { route.canExtend }
    var explanation by remember { mutableStateOf(false) }
    Column(modifier.widthIn(max = 220.dp)) {
        AdaptiveAssistChip(
            onClick = { if (route.canExtend) session.cycle(true) else explanation = !explanation },
            label = { Text(if (arm == null) countdown else stringResource(Res.string.cache_ttl_armed, arm)) },
        )
        AdaptiveTextButton(onClick = { explanation = !explanation }) {
            Text(stringResource(Res.string.cache_ttl_info), style = MaterialTheme.typography.labelSmall)
        }
        if (explanation) {
            Text(stringResource(if (route.canExtend) Res.string.cache_ttl_help else Res.string.cache_ttl_readonly),
                style = MaterialTheme.typography.bodySmall)
            if (route.endpoint in marketplaces) {
                Text(stringResource(Res.string.cache_ttl_market), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
