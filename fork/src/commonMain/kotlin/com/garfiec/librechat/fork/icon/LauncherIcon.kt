package com.garfiec.librechat.fork.icon

import androidx.compose.runtime.Composable

/** The sigil's palettes. [alias] is the class part of the launcher alias in `:fork`'s Android manifest. */
enum class LauncherAlloy(val alias: String, val displayName: String) {
    EMBER_ALCHEMY("EmberAlchemy", "Ember-alchemy"),
    COLD_LAB("ColdLab", "Cold-lab"),
    NIGHT_GLASS("NightGlass", "Night-glass"),
    ;

    companion object {
        /** The brand's icon: the alias the manifest enables, and the application's own icon. */
        val DEFAULT = EMBER_ALCHEMY
    }
}

/**
 * Swaps the app's launcher icon. A launcher treats the swap as one app entry leaving and another
 * arriving, and the system may end an app whose entry it was started through, so a pick is not
 * applied under the user's hands: it waits until the app leaves the screen, or for [closeNow].
 */
interface LauncherIconSwitcher {
    /** The icon in force, counting a pick that is still waiting. */
    val current: LauncherAlloy

    fun pick(alloy: LauncherAlloy)

    /** Applies the waiting pick and closes the app. */
    fun closeNow()
}

/** Whether this platform can swap its launcher icon; a settings row is shown only where it can. */
expect val launcherIconChoice: Boolean

@Composable
expect fun rememberLauncherIconSwitcher(): LauncherIconSwitcher?
