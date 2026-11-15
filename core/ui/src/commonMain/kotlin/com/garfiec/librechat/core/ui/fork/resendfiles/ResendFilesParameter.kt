package com.garfiec.librechat.core.ui.fork.resendfiles

import com.garfiec.librechat.core.model.ParameterDefinition
import com.garfiec.librechat.core.model.ParameterType
import com.garfiec.librechat.core.model.fork.ResendFiles
import com.garfiec.librechat.core.ui.components.ModelParameters

// The English texts are the keys ParameterDescriptions translates by.
const val RESEND_FILES_PLACEHOLDER = "All"
const val RESEND_FILES_DESCRIPTION =
    "How many of the most recent earlier files are sent again with each message. Leave blank for all, " +
        "enter 0 for none. Files on the new message are always sent, and the model is told which earlier " +
        "files it cannot see."

/**
 * Upstream's Resend Files switch as the count field the operator's server takes: blank resends
 * every earlier file, 0 none, N the most recent N. The field's text is what [ModelParameters]
 * reads and writes under the key; requests and agents encode it with `resendFilesWire`.
 */
fun asCount(definition: ParameterDefinition): ParameterDefinition = definition.copy(
    type = ParameterType.TEXT,
    default = "",
    placeholder = RESEND_FILES_PLACEHOLDER,
    description = RESEND_FILES_DESCRIPTION,
)

// Plain functions, not extensions: a hook line names them in full and adds no import.

fun resendFilesOf(params: ModelParameters): ResendFiles = ResendFiles.of(params.resendFiles, params.resendFilesCount)

/** [params] with [choice]; a preset or conversation that names none leaves them as they are. */
fun withResendFiles(params: ModelParameters, choice: ResendFiles?): ModelParameters =
    if (choice == null) params else params.copy(resendFiles = choice.any, resendFilesCount = choice.count)

/** The field's text: blank for every file, so an untouched field is unset and nothing is sent. */
fun resendFilesText(params: ModelParameters): String = when (val choice = resendFilesOf(params)) {
    ResendFiles.ALL -> ""
    else -> choice.limit.toString()
}

fun withResendFilesText(params: ModelParameters, text: String): ModelParameters =
    withResendFiles(params, ResendFiles.parse(text))
