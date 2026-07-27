package com.garfiec.librechat.core.data.db

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConversationTagQueryTest : AccountIsolationTestBase() {

    @Test
    fun `exact saved token query spans archive states and remains account scoped`() = runTest {
        val dao = db.conversationDao()
        dao.upsert(conversation("active-saved", accountA, tags = """["Saved"]"""))
        dao.upsert(
            conversation(
                "archived-saved",
                accountA,
                isArchived = true,
                tags = """["work","Saved"]""",
            ),
        )
        dao.upsert(conversation("substring-only", accountA, tags = """["Saved for later"]"""))
        dao.upsert(conversation("other-account", accountB, tags = """["Saved"]"""))

        val result = dao.getConversationsWithTagForAccount(accountA, "\"Saved\"")

        assertThat(result.map { it.conversationId })
            .containsExactly("active-saved", "archived-saved")
    }
}
