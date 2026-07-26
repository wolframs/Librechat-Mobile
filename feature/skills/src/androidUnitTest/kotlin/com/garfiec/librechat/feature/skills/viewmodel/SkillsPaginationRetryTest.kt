package com.garfiec.librechat.feature.skills.viewmodel

import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.repository.RoleRepository
import com.garfiec.librechat.core.data.repository.SkillsRepository
import com.garfiec.librechat.core.model.SkillSummary
import com.garfiec.librechat.core.model.response.SkillListResponse
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
class SkillsPaginationRetryTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<SkillsRepository>(relaxed = true)
    private val roleRepository = mockk<RoleRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { roleRepository.userPermissions } returns MutableStateFlow(null)
        coEvery { repository.listSkills(search = null, cursor = null) } returns Result.Success(
            SkillListResponse(
                skills = listOf(SkillSummary(id = "a", name = "skill-a")),
                hasMore = true,
                after = "cursor-1",
            ),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `failed cursor is latched until explicit retry and existing skills stay visible`() =
        runTest(dispatcher) {
            coEvery {
                repository.listSkills(search = null, cursor = "cursor-1")
            } returns Result.Error(message = "Page unavailable") andThen Result.Success(
                SkillListResponse(
                    skills = listOf(SkillSummary(id = "b", name = "skill-b")),
                    hasMore = false,
                    after = null,
                ),
            )
            val viewModel = SkillsListViewModel(repository, roleRepository)
            viewModel.loadFirstPage()
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.skills.map { it.id }).containsExactly("a")
            assertThat(viewModel.uiState.value.loadMoreError).isEqualTo("Page unavailable")
            assertThat(viewModel.uiState.value.error).isNull()
            coVerify(exactly = 1) {
                repository.listSkills(search = null, cursor = "cursor-1")
            }

            viewModel.retryLoadMore()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.skills.map { it.id }).containsExactly("a", "b").inOrder()
            assertThat(viewModel.uiState.value.loadMoreError).isNull()
            coVerify(exactly = 2) {
                repository.listSkills(search = null, cursor = "cursor-1")
            }
        }
}
