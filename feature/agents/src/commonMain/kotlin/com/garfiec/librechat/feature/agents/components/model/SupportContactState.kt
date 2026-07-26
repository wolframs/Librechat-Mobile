package com.garfiec.librechat.feature.agents.components.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SupportContactState(
    val name: String = "",
    val email: String = "",
)
