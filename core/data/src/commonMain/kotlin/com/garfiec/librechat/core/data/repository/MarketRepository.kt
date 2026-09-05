package com.garfiec.librechat.core.data.repository

import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.common.result.safeApiCall
import com.garfiec.librechat.core.model.MarketPrice
import com.garfiec.librechat.core.model.MarketplaceEndpoints
import com.garfiec.librechat.core.network.api.MarketApi

interface MarketRepository {
    suspend fun endpoints(): Result<MarketplaceEndpoints>
    suspend fun price(model: String): Result<MarketPrice>
}

class MarketRepositoryImpl(private val api: MarketApi) : MarketRepository {
    override suspend fun endpoints(): Result<MarketplaceEndpoints> = safeApiCall { api.endpoints() }
    override suspend fun price(model: String): Result<MarketPrice> = safeApiCall { api.price(model) }
}
