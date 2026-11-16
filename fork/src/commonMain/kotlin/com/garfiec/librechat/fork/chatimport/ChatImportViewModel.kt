package com.garfiec.librechat.fork.chatimport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.common.identity.currentAccountId
import com.garfiec.librechat.core.common.result.safeApiCall
import com.garfiec.librechat.core.common.result.getOrThrow
import com.garfiec.librechat.core.data.repository.ConversationRepository
import com.garfiec.librechat.core.network.fork.chatimport.ChatImportApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import org.koin.compose.koinInject
import kotlin.time.Clock

class ChatImportHandoff {
    val results = Channel<String?>(Channel.BUFFERED)
}

@Composable
fun ChatImportNavigation(onImported: (String?) -> Unit, handoff: ChatImportHandoff = koinInject()) {
    val callback = rememberUpdatedState(onImported)
    LaunchedEffect(handoff) { handoff.results.receiveAsFlow().collect { callback.value(it) } }
}

enum class ImportStatus { IDLE, IMPORTING, SUCCEEDED, FAILED, INVALID_FILE }

class ChatImportViewModel(
    private val api: ChatImportApi,
    private val conversations: ConversationRepository,
    private val handoff: ChatImportHandoff,
    private val accounts: ActiveAccountProvider,
) : ViewModel() {
    private val mutableStatus = MutableStateFlow(ImportStatus.IDLE)
    val status = mutableStatus.asStateFlow()

    fun dismissStatus() { mutableStatus.value = ImportStatus.IDLE }

    fun importChat(document: ImportDocument) {
        if (mutableStatus.value == ImportStatus.IMPORTING) return
        if (document.name.substringAfterLast('.').lowercase() !in setOf("md", "markdown", "json")) {
            mutableStatus.value = ImportStatus.INVALID_FILE
            return
        }
        val account = accounts.currentAccountId() ?: return
        mutableStatus.value = ImportStatus.IMPORTING
        viewModelScope.launch {
            val result = safeApiCall {
                val bytes = requireNotNull(document.read())
                check(accounts.currentAccountId() == account)
                val offset = -TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds / 60
                val ack = api.upload(bytes, document.name, offset)
                check(accounts.currentAccountId() == account)
                ack.conversationId?.takeIf { it.isNotBlank() }?.let {
                    conversations.refreshConversation(it, account).getOrThrow()
                }
                ack.conversationId?.takeIf { it.isNotBlank() }
            }
            if (accounts.currentAccountId() != account) {
                mutableStatus.value = ImportStatus.IDLE
                return@launch
            }
            when (result) {
                is com.garfiec.librechat.core.common.result.Result.Success -> {
                    mutableStatus.value = if (result.data == null) ImportStatus.SUCCEEDED else ImportStatus.IDLE
                    handoff.results.send(result.data)
                }
                else -> mutableStatus.value = ImportStatus.FAILED
            }
        }
    }
}
