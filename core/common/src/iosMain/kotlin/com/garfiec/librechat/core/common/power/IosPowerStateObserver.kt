package com.garfiec.librechat.core.common.power

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoPowerStateDidChangeNotification
import platform.Foundation.isLowPowerModeEnabled

class IosPowerStateObserver : PowerStateObserver {

    override val isPowerConstrained: Flow<Boolean> = callbackFlow {
        fun emitCurrent() {
            trySend(NSProcessInfo.processInfo.isLowPowerModeEnabled())
        }

        emitCurrent()

        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSProcessInfoPowerStateDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> emitCurrent() }

        awaitClose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }.distinctUntilChanged()
}
