package com.garfiec.librechat.fork.keyerrors

import com.garfiec.librechat.core.model.error.UserKeyError
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThreadKeyErrorTest {
    @Test fun recognizesAllFiveKeyErrors() {
        assertEquals(UserKeyError.NoUserKey(null), ThreadKeyError.parse("""{"type":"no_user_key"}"""))
        assertEquals(UserKeyError.InvalidUserKey(null), ThreadKeyError.parse("""{"type":"invalid_user_key"}"""))
        assertEquals(UserKeyError.NoBaseUrl(null), ThreadKeyError.parse("""{"type":"no_base_url"}"""))
        assertEquals(UserKeyError.InvalidBaseUrl(null), ThreadKeyError.parse("""{"type":"invalid_base_url"}"""))
        assertEquals(
            UserKeyError.ExpiredUserKey("openAI", "2026-11-16T12:00:00Z"),
            ThreadKeyError.parse("""{"type":"expired_user_key","endpoint":"openAI","expiredAt":"2026-11-16T12:00:00Z"}"""),
        )
    }

    @Test fun expiryWithoutADateUsesTheExistingInvalidKeyMessage() {
        assertEquals(
            UserKeyError.InvalidUserKey("openAI"),
            ThreadKeyError.parse("""{"type":"expired_user_key","endpoint":"openAI"}"""),
        )
    }

    @Test fun unrelatedAndMalformedErrorsKeepTheirExistingRendering() {
        listOf("plain error", "no_user_key", "{}", "[]", """{"type":"future_error"}""", """{"type":{}}""")
            .forEach { assertNull(ThreadKeyError.parse(it), it) }
    }
}
