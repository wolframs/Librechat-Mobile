package com.garfiec.librechat.fork.icon

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual val launcherIconChoice: Boolean = true

@Composable
actual fun rememberLauncherIconSwitcher(): LauncherIconSwitcher? {
    val activity = LocalContext.current.findActivity() ?: return null
    return remember(activity) { AndroidLauncherIconSwitcher(activity) }
}

/** The alloy whose launcher alias is enabled: the icon the launcher shows. Null if none is. */
fun enabledLauncherAlloyOrNull(context: Context): LauncherAlloy? = LauncherAlloy.entries.firstOrNull { isEnabled(context, it) }

private fun enabledLauncherAlloy(context: Context): LauncherAlloy = enabledLauncherAlloyOrNull(context) ?: LauncherAlloy.DEFAULT

private const val ALIAS_PACKAGE = "com.garfiec.librechat.fork.icon."

private fun component(context: Context, alloy: LauncherAlloy) = ComponentName(context.packageName, ALIAS_PACKAGE + alloy.alias)

private fun isEnabled(context: Context, alloy: LauncherAlloy): Boolean =
    when (context.packageManager.getComponentEnabledSetting(component(context, alloy))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        // Untouched since install: the manifest decides, and it enables the default alone.
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> alloy == LauncherAlloy.DEFAULT
        else -> false
    }

/** The new entry is enabled before the others are disabled, so the app is never without one. */
private fun apply(context: Context, alloy: LauncherAlloy) {
    val packages = context.packageManager
    val order = listOf(alloy) + (LauncherAlloy.entries - alloy)
    order.forEach {
        val state = if (it == alloy) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        packages.setComponentEnabledSetting(component(context, it), state, PackageManager.DONT_KILL_APP)
    }
}

/** One waiting pick for the process, applied when its last visible activity stops. */
private object WaitingPick : Application.ActivityLifecycleCallbacks {
    var alloy: LauncherAlloy? = null
    private var watching = false

    fun wait(application: Application, pick: LauncherAlloy) {
        alloy = pick.takeIf { it != enabledLauncherAlloy(application) }
        if (!watching) {
            watching = true
            application.registerActivityLifecycleCallbacks(this)
        }
    }

    fun applyNow(context: Context) {
        alloy?.let { apply(context.applicationContext, it) }
        alloy = null
    }

    override fun onActivityStopped(activity: Activity) {
        // A rotation stops the activity too; the app has not left the screen then.
        if (!activity.isChangingConfigurations) applyNow(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

private class AndroidLauncherIconSwitcher(private val activity: Activity) : LauncherIconSwitcher {
    override val current: LauncherAlloy get() = WaitingPick.alloy ?: enabledLauncherAlloy(activity)

    override fun pick(alloy: LauncherAlloy) = WaitingPick.wait(activity.application, alloy)

    override fun closeNow() {
        WaitingPick.applyNow(activity)
        activity.finishAndRemoveTask()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
