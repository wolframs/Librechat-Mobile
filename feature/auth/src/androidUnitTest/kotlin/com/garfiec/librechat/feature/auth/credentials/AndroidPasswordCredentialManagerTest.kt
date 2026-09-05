package com.garfiec.librechat.feature.auth.credentials

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class AndroidPasswordCredentialManagerTest {

    @Test
    fun `retrieve forwards the scoped ids and returns the selected password`() = runTest {
        val gateway = FakeGateway().apply {
            credential = PasswordCredential("alice · server", "secret")
        }
        val manager = AndroidPasswordCredentialManager(gateway)

        val result = manager.getCredential(setOf("alice · server", "bob · server"))

        assertThat(gateway.requestedIds).containsExactly("alice · server", "bob · server")
        assertThat(result).isEqualTo(PasswordCredential("alice · server", "secret"))
    }

    @Test
    fun `retrieve allows provider selection when no ids are registered`() = runTest {
        val gateway = FakeGateway()
        val manager = AndroidPasswordCredentialManager(gateway)

        assertThat(manager.getCredential(emptySet())).isNull()
        assertThat(gateway.retrieveCalls).isEqualTo(1)
        assertThat(gateway.requestedIds).isEmpty()
    }

    @Test
    fun `retrieve provider failure is a benign empty result`() = runTest {
        val gateway = FakeGateway().apply {
            retrieveFailure = IllegalStateException("provider unavailable")
        }

        assertThat(AndroidPasswordCredentialManager(gateway).getCredential(setOf("alice"))).isNull()
    }

    @Test
    fun `create and update both use the current password request`() = runTest {
        val gateway = FakeGateway()
        val manager = AndroidPasswordCredentialManager(gateway)
        val first = PasswordSaveRequest("alice · server", "first")
        val updated = PasswordSaveRequest("alice · server", "updated")

        assertThat(manager.saveCredential(first)).isTrue()
        assertThat(manager.saveCredential(updated)).isTrue()

        assertThat(gateway.savedRequests).containsExactly(first, updated).inOrder()
    }

    @Test
    fun `save provider failure does not block successful login`() = runTest {
        val gateway = FakeGateway().apply {
            saveFailure = IllegalStateException("provider unavailable")
        }

        val saved = AndroidPasswordCredentialManager(gateway)
            .saveCredential(PasswordSaveRequest("alice", "secret"))

        assertThat(saved).isFalse()
    }

    @Test
    fun `coroutine cancellation is never converted into provider dismissal`() = runTest {
        val retrieveGateway = FakeGateway().apply {
            retrieveFailure = CancellationException("cancel retrieve")
        }
        assertFailsWith<CancellationException> {
            AndroidPasswordCredentialManager(retrieveGateway).getCredential(setOf("alice"))
        }

        val saveGateway = FakeGateway().apply {
            saveFailure = CancellationException("cancel save")
        }
        assertFailsWith<CancellationException> {
            AndroidPasswordCredentialManager(saveGateway)
                .saveCredential(PasswordSaveRequest("alice", "secret"))
        }
    }

    private class FakeGateway : AndroidPasswordCredentialGateway {
        var credential: PasswordCredential? = null
        var retrieveFailure: Throwable? = null
        var saveFailure: Throwable? = null
        var requestedIds: Set<String> = emptySet()
        var retrieveCalls: Int = 0
        val savedRequests = mutableListOf<PasswordSaveRequest>()

        override suspend fun getPassword(allowedIds: Set<String>): PasswordCredential? {
            retrieveCalls++
            requestedIds = allowedIds
            retrieveFailure?.let { throw it }
            return credential
        }

        override suspend fun savePassword(request: PasswordSaveRequest) {
            saveFailure?.let { throw it }
            savedRequests += request
        }
    }
}
