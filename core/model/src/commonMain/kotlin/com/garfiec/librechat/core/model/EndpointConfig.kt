package com.garfiec.librechat.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EndpointConfig(
    val provider: String? = null,
    val extendedCacheTTL: Boolean? = null,
    val customParams: CustomEndpointParams? = null,
    val type: String? = null,
    val order: Int? = null,
    val iconURL: String? = null,
    val modelDisplayLabel: String? = null,
    val name: String? = null,
    val userProvide: Boolean? = null,
    val userProvideURL: Boolean? = null,
    val capabilities: List<String> = emptyList(),
    val disableBuilder: Boolean? = null,
    val azure: Boolean? = null,
)

@Serializable
data class CustomEndpointParams(
    val defaultParamsEndpoint: String? = null,
    val paramDefinitions: List<JsonObject> = emptyList(),
)
