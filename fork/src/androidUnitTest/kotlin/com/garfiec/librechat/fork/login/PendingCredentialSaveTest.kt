package com.garfiec.librechat.fork.login

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PendingCredentialSaveTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun `two factor saves only after verification consumes once and clears when owner ends`() = runTest {
        val data = PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("pending.preferences_pb") }
        val store = SavedLoginStore(data)
        val handoff = PendingCredentialSaveHandoff()
        val pending = pendingSave("https://one", "alice", "secret", null)!!
        var saves = 0
        var navigations = 0
        val manager = object : PasswordCredentialManager {
            override suspend fun getCredential(allowedIds: Set<String>): PasswordCredential? = null
            override suspend fun saveCredential(request: PasswordSaveRequest): Boolean {
                saves++
                assertEquals("alice", request.id)
                return true
            }
        }
        handoff.stage("token", pending)
        val bridge = TwoFactorSaveBridge(handoff, "token", store)
        assertEquals(0, saves)
        bridge.complete(manager) { navigations++ }
        bridge.complete(manager) { navigations++ }
        assertEquals(1, saves)
        assertEquals(2, navigations)
        assertEquals("alice", store.refs("https://one").single().credentialId)

        handoff.stage("other", pending)
        val abandoned = TwoFactorSaveBridge(handoff, "other", store)
        val owner = ViewModelStore()
        owner.put("save", abandoned)
        owner.clear()
        abandoned.complete(manager) { }
        assertEquals(1, saves)
    }

    @Test
    fun `unchanged selection skips save but changed username or password uses plain username`() {
        val picked = PasswordCredential("alice", "old")
        assertNull(pendingSave("https://one", " alice ", "old", picked))
        assertEquals("alice", pendingSave("https://one", "alice", "new", picked)?.ref?.credentialId)
        assertEquals("bob", pendingSave("https://one", "bob", "old", picked)?.ref?.credentialId)
        assertNull(pendingSave("https://one", "", "old", null))
    }

    @Test
    fun `handoff replaces consumes once rejects another token and clears on abandonment`() {
        val handoff = PendingCredentialSaveHandoff()
        val pending = pendingSave("https://one", "alice", "secret", null)!!
        handoff.stage("old", pending)
        handoff.stage("new", pending)
        assertSame(pending, handoff.consume("new"))
        assertNull(handoff.consume("new"))
        handoff.stage("new", pending)
        assertNull(handoff.consume("other"))
        assertNull(handoff.consume("new"))
        handoff.stage("new", pending)
        handoff.clear()
        assertNull(handoff.consume("new"))
    }
}
