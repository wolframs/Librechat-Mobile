package com.garfiec.librechat.core.data.prefetch

import platform.BackgroundTasks.BGTaskRequest
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The submission gate, observed through the seam rather than through `BGTaskScheduler`: a test
 * process has no app bundle, so the real scheduler answers every request with an error and would
 * let a missing gate pass unnoticed. In the app the same call with no handler registered throws,
 * which is how 2026.10.6 died on every signed-in cold launch.
 */
class IosPrefetchSchedulerTest {

    private val identifiers = listOf(IosPrefetchTasks.REFRESH_ID, IosPrefetchTasks.PROCESSING_ID)

    private val defaults get() = NSUserDefaults.standardUserDefaults

    private fun dueKey(identifier: String) = "prefetch_task_due_$identifier"

    private class RecordingSubmitter : BackgroundTaskSubmitter {
        val submitted = mutableListOf<BGTaskRequest>()
        override fun submit(request: BGTaskRequest): BackgroundTaskRefusal? {
            submitted += request
            return null
        }
    }

    @BeforeTest
    fun clearDueDates() = identifiers.forEach { defaults.removeObjectForKey(dueKey(it)) }

    @AfterTest
    fun tidy() = clearDueDates()

    @Test
    fun `nothing reaches the system scheduler while the launch handlers are unregistered`() {
        val submitter = RecordingSubmitter()
        val scheduler = IosPrefetchScheduler(handlersRegistered = { false }, submitter = submitter)

        scheduler.ensureScheduled(allowMetered = true)

        assertTrue(submitter.submitted.isEmpty())
        // No due date either, so the next emission starts from scratch rather than inheriting one.
        identifiers.forEach { assertNull(defaults.objectForKey(dueKey(it))) }
    }

    @Test
    fun `both requests are submitted once the launch handlers are registered`() {
        val submitter = RecordingSubmitter()
        val scheduler = IosPrefetchScheduler(handlersRegistered = { true }, submitter = submitter)
        val before = platform.Foundation.NSDate().timeIntervalSince1970

        scheduler.ensureScheduled(allowMetered = true)

        assertEquals(identifiers, submitter.submitted.map { it.identifier })
        identifiers.forEach { identifier ->
            val due = defaults.doubleForKey(dueKey(identifier))
            assertTrue(due > before, "$identifier should have a due date in the future")
            val request = submitter.submitted.first { it.identifier == identifier }
            assertEquals(due, assertNotNull(request.earliestBeginDate).timeIntervalSince1970)
        }
    }

    @Test
    fun `a due date already stored is reused rather than pushed out`() {
        val submitter = RecordingSubmitter()
        val scheduler = IosPrefetchScheduler(handlersRegistered = { true }, submitter = submitter)
        val stored = platform.Foundation.NSDate().timeIntervalSince1970 + 60.0
        defaults.setDouble(stored, dueKey(IosPrefetchTasks.REFRESH_ID))

        scheduler.ensureScheduled(allowMetered = true)

        val refresh = submitter.submitted.first { it.identifier == IosPrefetchTasks.REFRESH_ID }
        assertEquals(stored, assertNotNull(refresh.earliestBeginDate).timeIntervalSince1970)
    }
}
