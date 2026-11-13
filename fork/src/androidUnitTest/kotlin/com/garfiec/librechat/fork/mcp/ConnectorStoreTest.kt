package com.garfiec.librechat.fork.mcp

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConnectorStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun `offline hints distinguish unknown empty and switched off and isolate accounts`() = runTest {
        val data = PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("mcp.preferences_pb") }
        val store = ConnectorStore(data)
        assertNull(store.read("one", "chat"))
        store.remember("one", "chat", setOf("web"))
        store.remember("one", "other", emptySet())
        assertEquals(setOf("web"), ConnectorStore(data).read("one", "chat"))
        assertEquals(emptySet(), store.read("one", "other"))
        assertNull(store.read("two", "chat"))
        repeat(100) { store.remember("one", "chat-$it", emptySet()) }
        assertNull(store.read("one", "chat"))
        assertEquals(emptySet(), store.read("one", "chat-99"))
    }
}
