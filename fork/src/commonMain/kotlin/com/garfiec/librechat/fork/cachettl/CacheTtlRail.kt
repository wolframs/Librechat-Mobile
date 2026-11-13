package com.garfiec.librechat.fork.cachettl

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.data.fork.cachettl.CacheTtlMarkerStore
import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.Message
import com.garfiec.librechat.core.ui.components.AdaptiveAlertDialog
import com.garfiec.librechat.core.ui.components.AdaptiveTextButton
import com.garfiec.librechat.fork.resources.Res
import com.garfiec.librechat.fork.resources.cache_ttl_armed
import com.garfiec.librechat.fork.resources.cache_ttl_close
import com.garfiec.librechat.fork.resources.cache_ttl_help
import com.garfiec.librechat.fork.resources.cache_ttl_info
import com.garfiec.librechat.fork.resources.cache_ttl_market
import com.garfiec.librechat.fork.resources.cache_ttl_readonly
import com.garfiec.librechat.fork.resources.cache_ttl_semantics
import com.garfiec.librechat.fork.resources.composer_cache
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
    marketplace: Boolean = false,
) {
    val route = cacheTtlRoute(endpoint, model, configs, parameters, agentProvider, agentModel, agentParameters)
    LaunchedEffect(route.canExtend) { session.reconcile(route.canExtend) }
    if (!route.shown) return
    val store = koinInject<CacheTtlMarkerStore>()
    val accounts = koinInject<ActiveAccountProvider>()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val markers by remember(store, accounts) { store.observe(accounts) }.collectAsState(emptyList())
    val currentPath by remember(state) { state.map(path).distinctUntilChanged() }.collectAsState(path(state.value))
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { now = Clock.System.now().toEpochMilliseconds(); delay(1000) }
        }
    }
    val anchor = cacheTtlAnchor(currentPath, markers)
    val remaining = anchor?.let { remainingMillis(it, now) }
    val countdown = when {
        remaining == null -> ""
        remaining <= 0 -> ""
        else -> formatRemaining(remaining)
    }
    val arm = session.arm.takeIf { route.canExtend }
    var explanation by remember { mutableStateOf(false) }
    val label = if (arm == null) countdown else stringResource(Res.string.cache_ttl_armed, arm)
    if (label.isBlank()) return
    CacheTtlPill(
        label = label,
        onClick = { if (route.canExtend) session.cycle(true) else explanation = true },
        onLongClick = { explanation = true },
        modifier = modifier,
    )
    if (explanation) {
        AdaptiveAlertDialog(
            onDismissRequest = { explanation = false },
            confirmButton = {
                AdaptiveTextButton(onClick = { explanation = false }) { Text(stringResource(Res.string.cache_ttl_close)) }
            },
            title = { Text(stringResource(Res.string.cache_ttl_info)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(if (route.canExtend) Res.string.cache_ttl_help else Res.string.cache_ttl_readonly))
                    if (marketplace) Text(stringResource(Res.string.cache_ttl_market))
                }
            },
        )
    }
}

@Composable
private fun CacheTtlPill(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.cache_ttl_semantics, label)
    Box(modifier.height(48.dp).combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.composer_cache) + " " + label,
            Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
