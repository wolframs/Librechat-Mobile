package com.garfiec.librechat.feature.agents.components.model

import com.garfiec.librechat.core.model.ArtifactsMode
import kotlinx.serialization.Serializable

@Serializable
data class AgentCapabilities(
    val artifactsMode: ArtifactsMode? = null,
    val endAfterTools: Boolean = false,
    val hideSequentialOutputs: Boolean = false,
    val recursionLimit: Int = 25,
)
