package com.garfiec.librechat.feature.auth.credentials

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import kotlinx.coroutines.CancellationException
import androidx.credentials.PasswordCredential as AndroidPasswordCredential

@Composable
actual fun rememberPasswordCredentialManager(): PasswordCredentialManager? {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return null
    return remember(activity) {
        AndroidPasswordCredentialManager(
            activity = activity,
            credentialManager = CredentialManager.create(activity),
        )
    }
}

private class AndroidPasswordCredentialManager(
    private val activity: Activity,
    private val credentialManager: CredentialManager,
) : PasswordCredentialManager {

    override suspend fun getCredential(allowedIds: Set<String>): PasswordCredential? {
        if (allowedIds.isEmpty()) return null
        return try {
            val response = credentialManager.getCredential(
                context = activity,
                request = GetCredentialRequest(
                    credentialOptions = listOf(
                        GetPasswordOption(allowedUserIds = allowedIds),
                    ),
                ),
            )
            val credential = response.credential as? AndroidPasswordCredential ?: return null
            PasswordCredential(
                id = credential.id,
                password = credential.password,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Cancellation, no matching credential, and unavailable providers are all benign here.
            null
        }
    }

    override suspend fun saveCredential(request: PasswordSaveRequest): Boolean =
        try {
            credentialManager.createCredential(
                context = activity,
                request = CreatePasswordRequest(
                    id = request.id,
                    password = request.password,
                ),
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The user can decline the system save sheet without affecting the successful login.
            false
        }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
