package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.repository.MarketRepository
import com.garfiec.librechat.core.model.MarketPrice
import com.garfiec.librechat.core.model.MarketRates
import com.garfiec.librechat.core.model.MarketReference
import com.garfiec.librechat.core.model.MarketplaceEndpoints
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<MarketRepository>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun discoveryDoesNotFetchPricesAndSwitchingServersClearsThePreviousResult() = runTest(dispatcher) {
        coEvery { repository.endpoints() } returns Result.Success(MarketplaceEndpoints(listOf("market")))
        coEvery { repository.price("model") } returns Result.Success(
            MarketPrice("model", MarketRates(input = 1.0), MarketReference(source = "provider")),
        )
        val vm = MarketViewModel(repository)
        vm.discover("https://a.example")
        advanceUntilIdle()
        assertThat(vm.state.value.available).isTrue()
        coVerify(exactly = 0) { repository.price(any()) }
        vm.load("market", "model")
        advanceUntilIdle()
        assertThat(vm.state.value.price?.model).isEqualTo("model")
        coEvery { repository.endpoints() } returns Result.Error(message = "not supported")
        vm.discover("https://b.example")
        assertThat(vm.state.value.price).isNull()
        assertThat(vm.state.value.available).isFalse()
        advanceUntilIdle()
        assertThat(vm.state.value.available).isFalse()
    }

    @Test fun missingQuoteClearsAnEarlierPriceAndNeverReportsFreeInference() = runTest(dispatcher) {
        coEvery { repository.endpoints() } returns Result.Success(MarketplaceEndpoints(listOf("market")))
        coEvery { repository.price(any()) } returns Result.Error(message = "model not in markets table")
        val vm = MarketViewModel(repository)
        vm.discover("https://a.example")
        advanceUntilIdle()
        vm.load("market", "missing")
        advanceUntilIdle()
        assertThat(vm.state.value.price).isNull()
        assertThat(vm.state.value.failed).isTrue()
        assertThat(vm.state.value.loading).isFalse()
    }
}
