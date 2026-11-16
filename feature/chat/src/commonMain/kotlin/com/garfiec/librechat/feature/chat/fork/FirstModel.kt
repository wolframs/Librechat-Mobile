package com.garfiec.librechat.feature.chat.fork

import com.garfiec.librechat.feature.chat.viewmodel.ModelSelectionHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Key resolution wakes the seeder without making key state a higher-priority selection tier. */
internal fun <T> firstModelKeyUpdates(input: Flow<T>, handle: ModelSelectionHandle): Flow<T> =
    combine(input, handle.stateFlow.map { it.endpointKeyStates }.distinctUntilChanged()) { value, _ -> value }
