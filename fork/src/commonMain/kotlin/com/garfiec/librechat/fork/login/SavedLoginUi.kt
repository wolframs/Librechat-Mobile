package com.garfiec.librechat.fork.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.garfiec.librechat.core.ui.components.AdaptiveTextButton
import com.garfiec.librechat.fork.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SavedLoginButton(
    bridge: PasswordManagerLoginBridge,
    enabled: Boolean,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val manager = rememberPasswordCredentialManager() ?: return
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    Column(modifier) {
        AdaptiveTextButton(enabled = enabled && !busy, onClick = {
            busy = true
            failed = false
            scope.launch {
                try {
                    bridge.pick(manager)?.let { onUsername(it.id); onPassword(it.password) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failed = true
                } finally {
                    busy = false
                }
            }
        }) { Text(stringResource(Res.string.saved_login_use)) }
        if (failed) Text(stringResource(Res.string.saved_login_storage_error))
    }
}

suspend fun rememberConnectedServer(url: String, store: SavedLoginStore) {
    try {
        store.rememberServer(url)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Remembering a shortcut must not block a successful connection.
    }
}

@Composable
fun rememberedServerStore(): SavedLoginStore = koinInject()

@Composable
fun RememberedServers(knownUrls: List<String>, enabled: Boolean, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val store = rememberedServerStore()
    val servers by store.rememberedServers.collectAsState(emptySet())
    val scope = rememberCoroutineScope()
    var failed by remember { mutableStateOf(false) }
    val remaining = rememberedServerList(servers, knownUrls)
    if (remaining.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.saved_login_servers))
        remaining.forEach { url ->
            Row(Modifier.fillMaxWidth()) {
                AdaptiveTextButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = { onSelect(url) }) { Text(url) }
                AdaptiveTextButton(enabled = enabled, onClick = {
                    scope.launch {
                        try {
                            store.forgetServer(url)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            failed = true
                        }
                    }
                }) { Text(stringResource(Res.string.saved_login_forget)) }
            }
        }
        if (failed) Text(stringResource(Res.string.saved_login_storage_error))
    }
}
