package com.garfiec.librechat.feature.chat.components.artifact

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * Receives [CONTENT_HEIGHT_REPORTER_JS]'s readings, bound as [CONTENT_HEIGHT_ANDROID_BRIDGE].
 * WebView calls a JavaScript interface on its own background thread, so each reading is handed
 * to the main thread before it reaches [onHeight].
 */
internal class ContentHeightJsBridge(private val onHeight: (cssPx: Float) -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun report(cssPx: Float) {
        if (cssPx.isNaN() || cssPx <= 0f) return
        main.post { onHeight(cssPx) }
    }
}
