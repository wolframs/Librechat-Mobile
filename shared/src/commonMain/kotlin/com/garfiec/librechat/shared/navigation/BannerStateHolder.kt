package com.garfiec.librechat.shared.navigation

import co.touchlab.kermit.Logger
import com.garfiec.librechat.core.common.identity.deriveServerId
import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.data.repository.BannerRepository
import com.garfiec.librechat.core.model.Banner
import com.garfiec.librechat.core.network.client.ServerUrlProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

class BannerStateHolder(
    private val bannerRepository: BannerRepository,
    private val settingsDataStore: SettingsDataStore,
    private val serverUrlProvider: ServerUrlProvider,
    private val scope: CoroutineScope,
) {

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val _dismissedBannerIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedBannerIds: StateFlow<Set<String>> = _dismissedBannerIds.asStateFlow()

    private var bannerServerId: String? = null

    fun fetchBanners() {
        scope.launch {
            try {
                val requestedServerId = currentServerId() ?: run {
                    clear()
                    return@launch
                }
                if (bannerServerId != null && bannerServerId != requestedServerId) {
                    clear()
                }
                val result = bannerRepository.getBanners()
                if (result is Result.Success) {
                    // A request started for server A must never publish after an account switch
                    // redirected the shared HTTP stack to server B.
                    if (currentServerId() != requestedServerId) return@launch
                    val now = Clock.System.now()
                    _dismissedBannerIds.value =
                        settingsDataStore.dismissedBannerIds(requestedServerId).first()
                    _banners.value = result.data.filter { banner ->
                        val from = banner.displayFrom?.let {
                            runCatching { Instant.parse(it) }
                                .onFailure { e -> Logger.w(e) { "Failed to parse banner displayFrom: $it" } }
                                .getOrNull()
                        }
                        val to = banner.displayTo?.let {
                            runCatching { Instant.parse(it) }
                                .onFailure { e -> Logger.w(e) { "Failed to parse banner displayTo: $it" } }
                                .getOrNull()
                        }
                        val afterStart = from == null || now >= from
                        val beforeEnd = to == null || now < to
                        afterStart && beforeEnd
                    }
                    bannerServerId = requestedServerId
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Failed to fetch banners" }
            }
        }
    }

    fun dismissBanner(bannerId: String) {
        val serverId = bannerServerId ?: return
        _dismissedBannerIds.update { it + bannerId }
        scope.launch {
            settingsDataStore.dismissBanner(serverId, bannerId)
        }
    }

    private fun currentServerId(): String? =
        runCatching {
            serverUrlProvider.getBaseUrl()
                .takeIf(String::isNotBlank)
                ?.let { deriveServerId(it).value }
        }.getOrNull()

    private fun clear() {
        _banners.value = emptyList()
        _dismissedBannerIds.value = emptySet()
        bannerServerId = null
    }
}
