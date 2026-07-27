package com.garfiec.librechat.feature.files.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import com.garfiec.librechat.core.ui.platform.currentTopmostViewController
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeContent
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private val activeFilePickerDelegates = mutableSetOf<NSObject>()

@Composable
actual fun rememberFilePickerLauncher(
    onFilePick: (fileRef: Any) -> Unit,
): FilePickerLauncher {
    val currentCallback = rememberUpdatedState(onFilePick)
    val launcher = remember { FilePickerLauncher() }
    DisposableEffect(launcher) {
        launcher.onPicked = { currentCallback.value(it) }
        onDispose { launcher.onPicked = null }
    }
    return launcher
}

actual class FilePickerLauncher {
    internal var onPicked: ((Any) -> Unit)? = null

    actual fun launch(mimeType: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val presenter = currentTopmostViewController() ?: run {
                Logger.w { "FilesPicker: no view controller available to present picker" }
                return@dispatch_async
            }
            // Files currently requests */*. Keeping the native picker at public.content also lets
            // providers expose cloud-backed documents without copying them into app memory.
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeContent),
            ).apply {
                allowsMultipleSelection = false
            }
            val delegate = FilePickerDelegate { onPicked?.invoke(it) }
            picker.delegate = delegate
            activeFilePickerDelegates.add(delegate)
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class FilePickerDelegate(
    private val onResult: (NSURL) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        try {
            (didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.let(onResult)
        } finally {
            activeFilePickerDelegates.remove(this)
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        activeFilePickerDelegates.remove(this)
    }
}
