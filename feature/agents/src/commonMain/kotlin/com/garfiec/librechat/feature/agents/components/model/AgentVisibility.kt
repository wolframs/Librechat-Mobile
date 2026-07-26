package com.garfiec.librechat.feature.agents.components.model

import kotlinx.serialization.Serializable

@Serializable
enum class AgentVisibility(val label: String) {
    PRIVATE("Private"),
    TEAM("Team"),
    PUBLIC("Public"),
}
