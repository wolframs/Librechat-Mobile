package com.garfiec.librechat.feature.files.viewmodel

import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.datastore.ServerDataStore
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.data.repository.FileRepository
import com.garfiec.librechat.core.model.response.EndpointFileConfig
import com.garfiec.librechat.core.model.response.FileUploadConfig
import com.garfiec.librechat.core.network.upload.StreamingUploadSource
import com.garfiec.librechat.feature.files.platform.FileReader
import com.google.common.truth.Truth.assertThat
import io.ktor.utils.io.ByteReadChannel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilesUploadStreamingTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<FileRepository>(relaxed = true)
    private val configRepository = mockk<com.garfiec.librechat.core.data.repository.ConfigRepository>(relaxed = true)
    private val reader = mockk<FileReader>(relaxed = true)
    private val serverDataStore = mockk<ServerDataStore>(relaxed = true)
    private val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
    private val viewMode = MutableStateFlow<String?>(null)
    private val sortField = MutableStateFlow<String?>(null)
    private val sortOrder = MutableStateFlow<String?>(null)
    private val fileRef = Any()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { configRepository.detectedBackendVersion } returns MutableStateFlow(null)
        coEvery { repository.getFiles() } returns Result.Success(emptyList())
        every { serverDataStore.getBaseUrl() } returns "https://chat.example.com"
        every { settingsDataStore.filesViewMode } returns viewMode
        every { settingsDataStore.filesSortField } returns sortField
        every { settingsDataStore.filesSortOrder } returns sortOrder
        every { reader.getFileName(fileRef) } returns "large.bin"
        every { reader.getMimeType(fileRef) } returns "application/octet-stream"
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = FilesViewModel(
        repository,
        configRepository,
        reader,
        serverDataStore,
        settingsDataStore,
        dispatcher,
    )

    @Test
    fun `known oversized file fails before its channel is opened or uploaded`() =
        runTest(dispatcher) {
            var opened = false
            every { reader.openUploadSource(fileRef) } returns StreamingUploadSource(2_048L) {
                opened = true
                ByteReadChannel.Empty
            }
            coEvery { repository.getFileConfig() } returns Result.Success(
                FileUploadConfig(
                    endpoints = mapOf(
                        "default" to EndpointFileConfig(fileSizeLimit = 1_024L),
                    ),
                ),
            )
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.uploadFile(fileRef)
            advanceUntilIdle()

            assertThat(opened).isFalse()
            assertThat(viewModel.uiState.value.isUploading).isFalse()
            assertThat(viewModel.uiState.value.error).contains("File is too large")
            coVerify(exactly = 0) {
                repository.uploadFile(
                    source = any(),
                    filename = any(),
                    type = any(),
                    fileId = any(),
                    endpoint = any(),
                    model = any(),
                    agentId = any(),
                    toolResource = any(),
                    messageFile = any(),
                    width = any(),
                    height = any(),
                    onProgress = any(),
                )
            }
        }

    @Test
    fun `cancel upload cancels the in-flight streaming repository call promptly`() =
        runTest(dispatcher) {
            every { reader.openUploadSource(fileRef) } returns StreamingUploadSource(null) {
                ByteReadChannel.Empty
            }
            coEvery { repository.getFileConfig() } returns Result.Error(message = "Older backend")
            val started = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            coEvery {
                repository.uploadFile(
                    source = any(),
                    filename = any(),
                    type = any(),
                    fileId = any(),
                    endpoint = any(),
                    model = any(),
                    agentId = any(),
                    toolResource = any(),
                    messageFile = any(),
                    width = any(),
                    height = any(),
                    onProgress = any(),
                )
            } coAnswers {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.uploadFile(fileRef)
            runCurrent()
            assertThat(started.isCompleted).isTrue()
            assertThat(viewModel.uiState.value.isUploading).isTrue()

            viewModel.cancelUpload()
            runCurrent()

            assertThat(cancelled.isCompleted).isTrue()
            assertThat(viewModel.uiState.value.isUploading).isFalse()
            assertThat(viewModel.uiState.value.uploadFilename).isEmpty()
        }
}
