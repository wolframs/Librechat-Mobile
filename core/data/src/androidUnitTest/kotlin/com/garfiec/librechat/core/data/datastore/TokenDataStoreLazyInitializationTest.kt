package com.garfiec.librechat.core.data.datastore

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TokenDataStoreLazyInitializationTest {

    @Test
    fun `constructor does not touch encrypted storage and warmup initializes once`() = runTest {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString(any(), null) } returns null
        var createCalls = 0

        val store = TokenDataStore(
            context = context,
            refreshClient = lazy { error("refresh client is not used during warm-up") },
            encryptedPrefsFactory = {
                createCalls += 1
                prefs
            },
        )

        assertThat(createCalls).isEqualTo(0)

        store.warmUp()
        store.warmUp()

        assertThat(createCalls).isEqualTo(1)
        assertThat(store.isAuthenticated).isFalse()
    }
}
