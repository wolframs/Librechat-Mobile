package com.garfiec.librechat.fork

import android.content.Context
import com.garfiec.librechat.R
import com.garfiec.librechat.fork.icon.LauncherAlloy
import com.garfiec.librechat.fork.icon.enabledLauncherAlloyOrNull

/**
 * The launcher icon in force, for anything that shows the app's icon beside the launcher;
 * [upstream] when no fork icon is enabled.
 */
fun launcherIconRes(context: Context, upstream: Int): Int = when (enabledLauncherAlloyOrNull(context)) {
    LauncherAlloy.COLD_LAB -> R.mipmap.fork_launcher_cold_lab
    LauncherAlloy.NIGHT_GLASS -> R.mipmap.fork_launcher_night_glass
    LauncherAlloy.EMBER_ALCHEMY -> R.mipmap.fork_launcher_ember_alchemy
    null -> upstream
}
