package com.garfiec.librechat.feature.agents.viewmodel

import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.datastore.ServerDataStore
import com.garfiec.librechat.core.data.repository.AgentRepository
import com.garfiec.librechat.core.data.repository.RoleRepository
import com.garfiec.librechat.core.data.util.PermissionGate
import com.garfiec.librechat.core.model.Agent
import com.garfiec.librechat.core.model.PaginatedAgents
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgentPaginationRetryTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AgentRepository>(relaxed = true)
    private val serverDataStore = mockk<ServerDataStore>(relaxed = true)
    private val roleRepository = mockk<RoleRepository>(relaxed = true)
    private val permissionGate = mockk<PermissionGate>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { roleRepository.userPermissions } returns MutableStateFlow(null)
        coEvery { permissionGate.awaitRole() } returns null
        every { serverDataStore.getBaseUrl() } returns "https://chat.example.com"
        coEvery { repository.getAgentCategories() } returns Result.Success(emptyList())
        coEvery {
            repository.getAgentsPaginated(page = 1, limit = any(), search = null, category = null)
        } returns Result.Success(
            PaginatedAgents(
                agents = listOf(Agent(id = "a", name = "Agent A")),
                hasMore = true,
                total = 2,
            ),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `failed page is latched until explicit retry and existing cards stay visible`() =
        runTest(dispatcher) {
            coEvery {
                repository.getAgentsPaginated(page = 2, limit = any(), search = null, category = null)
            } returns Result.Error(message = "Page unavailable") andThen Result.Success(
                PaginatedAgents(
                    agents = listOf(Agent(id = "b", name = "Agent B")),
                    hasMore = false,
                    total = 2,
                ),
            )
            val viewModel = AgentMarketplaceViewModel(
                repository,
                serverDataStore,
                roleRepository,
                permissionGate,
            )
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.agents.map { it.id }).containsExactly("a")
            assertThat(viewModel.uiState.value.loadMoreError).isEqualTo("Page unavailable")
            coVerify(exactly = 1) {
                repository.getAgentsPaginated(page = 2, limit = any(), search = null, category = null)
            }

            viewModel.retryLoadMore()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.agents.map { it.id }).containsExactly("a", "b").inOrder()
            assertThat(viewModel.uiState.value.loadMoreError).isNull()
            assertThat(viewModel.uiState.value.currentPage).isEqualTo(2)
            coVerify(exactly = 2) {
                repository.getAgentsPaginated(page = 2, limit = any(), search = null, category = null)
            }
        }
}
