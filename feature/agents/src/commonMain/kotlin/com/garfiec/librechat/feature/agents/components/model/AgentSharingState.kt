package com.garfiec.librechat.feature.agents.components.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AgentSharingState(
    val visibility: AgentVisibility = AgentVisibility.PRIVATE,
    val isCollaborative: Boolean = false,
)
