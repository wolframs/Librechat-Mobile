package com.garfiec.librechat.feature.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.repository.MarketRepository
import com.garfiec.librechat.core.model.MarketPrice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MarketUiState(
    val serverUrl: String = "",
    val selection: Pair<String, String?>? = null,
    val available: Boolean = false,
    val endpoints: List<String> = emptyList(),
    val loading: Boolean = false,
    val price: MarketPrice? = null,
    val failed: Boolean = false,
)

class MarketViewModel(private val repository: MarketRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(MarketUiState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0

    fun discover(serverUrl: String) {
        if (serverUrl.isBlank() || mutableState.value.serverUrl == serverUrl) return
        job?.cancel()
        val ticket = ++generation
        mutableState.value = MarketUiState(serverUrl = serverUrl)
        job = viewModelScope.launch {
            val result = repository.endpoints()
            if (ticket != generation) return@launch
            if (result is Result.Success) {
                mutableState.value = mutableState.value.copy(
                    available = true, endpoints = result.data.endpoints,
                )
            }
        }
    }

    /** Quotes are fetched only when the user opens or refreshes the price panel. */
    fun load(endpoint: String, model: String?) {
        job?.cancel()
        val ticket = ++generation
        val eligible = endpoint in mutableState.value.endpoints && !model.isNullOrBlank()
        mutableState.value = mutableState.value.copy(
            selection = endpoint to model, price = null, failed = false, loading = eligible,
        )
        if (!eligible || model == null) return
        job = viewModelScope.launch {
            val result = repository.price(model)
            if (ticket != generation) return@launch
            mutableState.value = mutableState.value.copy(
                loading = false,
                price = (result as? Result.Success)?.data?.takeIf { it.model == model },
                failed = result !is Result.Success || result.data.model != model,
            )
        }
    }
}
