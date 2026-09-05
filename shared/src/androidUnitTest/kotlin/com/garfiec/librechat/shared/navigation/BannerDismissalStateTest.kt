package com.garfiec.librechat.shared.navigation

import com.garfiec.librechat.core.common.identity.deriveServerId
import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.data.repository.BannerRepository
import com.garfiec.librechat.core.data.repository.ConfigRepository
import com.garfiec.librechat.core.data.repository.VersionCheckResult
import com.garfiec.librechat.core.model.Banner
import com.garfiec.librechat.core.network.client.ServerUrlProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BannerDismissalStateTest {

    @Test
    fun `announcement dismissal is restored for its server but not another server`() = runTest {
        val repository = mockk<BannerRepository>()
        val settings = mockk<SettingsDataStore>(relaxed = true)
        val serverUrlProvider = mockk<ServerUrlProvider>()
        val banner = Banner(bannerId = "same-id", message = "Maintenance")
        val serverA = "https://a.example.com"
        val serverB = "https://b.example.com"
        val serverAId = deriveServerId(serverA).value
        val serverBId = deriveServerId(serverB).value
        var currentUrl = serverA
        every { serverUrlProvider.getBaseUrl() } answers { currentUrl }
        every { settings.dismissedBannerIds(serverAId) } returns flowOf(setOf("same-id"))
        every { settings.dismissedBannerIds(serverBId) } returns flowOf(emptySet())
        coEvery { repository.getBanner() } returns Result.Success(banner)
        coEvery { serverUrlProvider.awaitBaseUrl() } answers { currentUrl }
        val holder = BannerStateHolder(repository, serverUrlProvider, this, settings)

        holder.fetchBanner()
        advanceUntilIdle()
        assertThat(holder.banner.value).isNull()

        currentUrl = serverB
        holder.fetchBanner()
        advanceUntilIdle()
        assertThat(holder.banner.value).isEqualTo(banner)

        holder.dismissBanner("same-id")
        advanceUntilIdle()
        coVerify { settings.dismissBanner(serverBId, "same-id") }
    }

    @Test
    fun `same incompatible version dismissed on server A remains visible on server B`() = runTest {
        val configRepository = mockk<ConfigRepository>(relaxed = true)
        val settings = mockk<SettingsDataStore>(relaxed = true)
        val serverUrlProvider = mockk<ServerUrlProvider>()
        val serverA = "https://a.example.com"
        val serverB = "https://b.example.com"
        val serverAId = deriveServerId(serverA).value
        val serverBId = deriveServerId(serverB).value
        var currentUrl = serverA
        every { serverUrlProvider.getBaseUrl() } answers { currentUrl }
        every { settings.dismissedVersionWarning(serverAId) } returns flowOf("v0.8.0")
        every { settings.dismissedVersionWarning(serverBId) } returns flowOf(null)
        coEvery { configRepository.checkBackendVersion() } returns Result.Success(
            VersionCheckResult(
                backendVersion = "v0.8.0",
                supportedVersion = "v0.9.0",
                isCompatible = false,
            ),
        )
        val holder = VersionCheckStateHolder(
            configRepository,
            settings,
            serverUrlProvider,
            this,
        )

        holder.checkBackendVersion()
        advanceUntilIdle()
        assertThat(holder.versionMismatch.value).isNull()

        currentUrl = serverB
        holder.checkBackendVersion()
        advanceUntilIdle()
        assertThat(holder.versionMismatch.value?.backendVersion).isEqualTo("v0.8.0")

        holder.dismissVersionWarningPermanently()
        advanceUntilIdle()
        coVerify { settings.setDismissedVersionWarning(serverBId, "v0.8.0") }
    }

    @Test
    fun `changed backend version resurfaces on the same server`() = runTest {
        val configRepository = mockk<ConfigRepository>(relaxed = true)
        val settings = mockk<SettingsDataStore>(relaxed = true)
        val serverUrlProvider = mockk<ServerUrlProvider>()
        val server = "https://a.example.com"
        val serverId = deriveServerId(server).value
        every { serverUrlProvider.getBaseUrl() } returns server
        every { settings.dismissedVersionWarning(serverId) } returns flowOf("v0.8.0")
        coEvery { configRepository.checkBackendVersion() } returns Result.Success(
            VersionCheckResult(
                backendVersion = "v0.8.1",
                supportedVersion = "v0.9.0",
                isCompatible = false,
            ),
        )
        val holder = VersionCheckStateHolder(
            configRepository,
            settings,
            serverUrlProvider,
            this,
        )

        holder.checkBackendVersion()
        advanceUntilIdle()

        assertThat(holder.versionMismatch.value?.backendVersion).isEqualTo("v0.8.1")
    }
}
