package com.garfiec.librechat.core.data.prefetch

import com.garfiec.librechat.core.logging.Diag
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.BackgroundTasks.BGTaskSchedulerErrorCodeUnavailable
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import kotlin.concurrent.Volatile

/**
 * The two background task identifiers, and what each is for.
 *
 * These are a four-way contract: this object submits them, `PrefetchBackgroundTasks.swift` registers
 * a handler for each, `Info.plist` permits them under `BGTaskSchedulerPermittedIdentifiers`, and iOS
 * matches all three by string. A mismatch is not a build error — the handler simply never runs, and
 * `BGTaskScheduler` reports nothing wrong.
 */
object IosPrefetchTasks {

    /**
     * Opportunistic top-up, scheduled at iOS's discretion. A refresh task cannot require power or a
     * network, so the prefetch gate is the only thing holding one back.
     */
    const val REFRESH_ID: String = "com.garfiec.librechat.prefetch.refresh"

    /** The bulk overnight pass — the direct counterpart of the Android periodic job. */
    const val PROCESSING_ID: String = "com.garfiec.librechat.prefetch.processing"

    /**
     * Under the ~30 seconds iOS allows a refresh task, covering the *whole* run including
     * [PrefetchBackgroundRunner]'s start handshake, with headroom left to unwind and record the
     * outcome — iOS kills the app for missing `setTaskCompleted`.
     */
    const val REFRESH_BUDGET_SECONDS: Double = 22.0

    /** Processing tasks get minutes, for the same reasons the Android worker's budget is minutes. */
    const val PROCESSING_BUDGET_SECONDS: Double = 300.0

    /** Earliest the system may consider each task. A floor, never a cadence — see [IosPrefetchScheduler]. */
    const val REFRESH_EARLIEST_SECONDS: Double = 2.0 * 60 * 60
    const val PROCESSING_EARLIEST_SECONDS: Double = 6.0 * 60 * 60

    /**
     * Whether `PrefetchBackgroundTasks.swift` has installed both launch handlers.
     *
     * Submitting a request whose identifier has no handler is an uncatchable
     * `NSInternalInconsistencyException`, not an `NSError`, so [IosPrefetchScheduler] refuses to
     * submit until this is set. The app registers before it starts Koin, which makes it true before
     * any submission is possible; the gate is the backstop for a reordering that reopens the race
     * (2026.10.6 crashed on every signed-in cold launch when registration ran after Koin).
     *
     * Volatile: set on the main thread, read from the coordinator's background dispatcher.
     */
    @Volatile
    var launchHandlersRegistered: Boolean = false
        private set

    fun markLaunchHandlersRegistered() {
        launchHandlersRegistered = true
    }
}

/**
 * Asks `BGTaskScheduler` to wake the app for a prefetch pass.
 *
 * Submission and cancellation live here rather than in Swift so that [PrefetchScheduleCoordinator]
 * drives both platforms. Only the launch handlers are Swift's, because only the app target can
 * install them before launch finishes.
 */
class IosPrefetchScheduler(
    /**
     * Whether a submission is safe yet — see [IosPrefetchTasks.launchHandlersRegistered]. Injected,
     * with the gate's one-way global as the default, so a test can hold it in either state.
     */
    private val handlersRegistered: () -> Boolean = { IosPrefetchTasks.launchHandlersRegistered },
    /**
     * The one `BGTaskScheduler` call this class makes. Behind a seam because a test process cannot
     * observe the real one: without an app bundle iOS answers every submission with an error, so a
     * test that lets the call through passes with or without the gate and shows nothing.
     */
    private val submitter: BackgroundTaskSubmitter = SystemBackgroundTaskSubmitter,
) : PrefetchScheduler {

    private val scheduler get() = BGTaskScheduler.sharedScheduler
    private val defaults get() = NSUserDefaults.standardUserDefaults

    /**
     * Whether iOS has last told us it will run background work at all.
     *
     * Optimistic until a refusal says otherwise: the readout hides the scheduled-run row when this is
     * false, and a row missing for a recoverable reason is worse than one briefly wrong. A plain
     * field — a stale read costs one status emission.
     */
    private var schedulingAvailable = true

    override val isSupported: Boolean get() = schedulingAvailable

    /**
     * The metered setting is deliberately ignored here.
     *
     * `BGProcessingTaskRequest.requiresNetworkConnectivity` can only say "wait for *a* network" — iOS
     * exposes no Wi-Fi-versus-cellular constraint the way `NetworkType.UNMETERED` does, so the choice
     * is enforced by the gate re-checking it when the task fires and declining the pass.
     */
    override fun ensureScheduled(allowMetered: Boolean) {
        submitDue(IosPrefetchTasks.REFRESH_ID, IosPrefetchTasks.REFRESH_EARLIEST_SECONDS) {
            BGAppRefreshTaskRequest(it)
        }
        submitDue(IosPrefetchTasks.PROCESSING_ID, IosPrefetchTasks.PROCESSING_EARLIEST_SECONDS) {
            BGProcessingTaskRequest(it).apply {
                // Both default to false. Without the network requirement iOS launches this with no
                // connection, the gate never opens, and the budget burns reporting nothing. Power is
                // politeness rather than gate alignment: this platform's power observer reads low
                // power mode only, so being off-charger does not close the gate the way it does on
                // Android.
                requiresExternalPower = true
                requiresNetworkConnectivity = true
            }
        }
    }

    override fun cancel() {
        for (identifier in IDENTIFIERS) {
            scheduler.cancelTaskRequestWithIdentifier(identifier)
            // Drop the due date too, so re-enabling later starts a fresh interval rather than
            // inheriting one set before the user switched the feature off.
            defaults.removeObjectForKey(dueKey(identifier))
        }
    }

    /**
     * Submits [identifier] for its stored due date, choosing a new one only once the old has passed.
     *
     * The date has to be persisted because what it defends against spans process death: this runs at
     * every process start, so recomputing `now + interval` would push the date back out each time and
     * a user who opens the app daily would keep resetting the clock, maturing neither task — the same
     * trap the Android side avoids with `KEEP`. Kept synchronous rather than checked against
     * `getPendingTaskRequests`, which is async and would race `cancel()` and the app's own suspension.
     */
    private fun submitDue(identifier: String, interval: Double, build: (String) -> BGTaskRequest) {
        val now = NSDate().timeIntervalSince1970
        val stored = defaults.doubleForKey(dueKey(identifier))
        val due = if (stored > now) stored else now + interval
        val request = build(identifier).apply {
            earliestBeginDate = NSDate.dateWithTimeIntervalSince1970(due)
        }
        if (submit(request)) {
            defaults.setDouble(due, dueKey(identifier))
        }
    }

    private fun submit(request: BGTaskRequest): Boolean {
        if (!handlersRegistered()) {
            // Not an error iOS reports: it throws, and the process dies. Skipping leaves whatever
            // request an earlier process submitted in place, and the due date unset, so the next
            // emission tries again.
            Diag.w(
                "Prefetch",
                attrs = mapOf("identifier" to request.identifier),
            ) { "background task launch handlers not registered yet; submission skipped" }
            return false
        }
        val refusal = submitter.submit(request)
        if (refusal == null) {
            schedulingAvailable = true
            return true
        }

        val code = refusal.code
        if (code == BGTaskSchedulerErrorCodeUnavailable) {
            // Background App Refresh is switched off for this app, so nothing will ever run — the
            // readout hides its scheduled-run row rather than showing "Never" beside no cause.
            schedulingAvailable = false
        }
        Diag.w(
            "Prefetch",
            attrs = mapOf(
                "identifier" to request.identifier,
                "code" to (code?.toString() ?: "unknown"),
            ),
        ) { "background task submission refused" }
        return false
    }

    private fun dueKey(identifier: String) = "$DUE_KEY_PREFIX$identifier"

    private companion object {
        val IDENTIFIERS = listOf(IosPrefetchTasks.REFRESH_ID, IosPrefetchTasks.PROCESSING_ID)
        const val DUE_KEY_PREFIX = "prefetch_task_due_"
    }
}

/** Why `BGTaskScheduler` declined a request; [code] is the `NSError` code, or null if it gave none. */
class BackgroundTaskRefusal(val code: Long?)

/** Hands a request to the system scheduler, answering null when it was accepted. */
fun interface BackgroundTaskSubmitter {
    fun submit(request: BGTaskRequest): BackgroundTaskRefusal?
}

private object SystemBackgroundTaskSubmitter : BackgroundTaskSubmitter {
    @OptIn(ExperimentalForeignApi::class)
    override fun submit(request: BGTaskRequest): BackgroundTaskRefusal? = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        if (BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error.ptr)) {
            null
        } else {
            BackgroundTaskRefusal(error.value?.code)
        }
    }
}
