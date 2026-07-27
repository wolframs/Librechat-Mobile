package com.garfiec.librechat.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * Image bytes materialized while PhotosUI owns the provider result.
 *
 * Feature modules receive this instead of a temporary NSURL, whose lifetime is not guaranteed after
 * the picker callback. It is intentionally iOS-only and recognized by the settings/agent content
 * readers; passwords and other sensitive content never use this bridge.
 */
data class IosPickedImage(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String,
)

private val activeImagePickerDelegates = mutableSetOf<NSObject>()

@Composable
fun rememberIosImagePickerLauncher(
    onImagePick: (IosPickedImage) -> Unit,
): IosImagePickerLauncher {
    val currentCallback = rememberUpdatedState(onImagePick)
    val launcher = remember { IosImagePickerLauncher() }
    DisposableEffect(launcher) {
        launcher.onPicked = { currentCallback.value(it) }
        onDispose { launcher.onPicked = null }
    }
    return launcher
}

class IosImagePickerLauncher internal constructor() {
    internal var onPicked: ((IosPickedImage) -> Unit)? = null

    fun launch() {
        dispatch_async(dispatch_get_main_queue()) {
            val presenter = currentTopmostViewController() ?: run {
                return@dispatch_async
            }
            val configuration = PHPickerConfiguration().apply {
                selectionLimit = 1
                filter = PHPickerFilter.imagesFilter
            }
            val picker = PHPickerViewController(configuration)
            val delegate = IosImagePickerDelegate { picked -> onPicked?.invoke(picked) }
            picker.delegate = delegate
            activeImagePickerDelegates.add(delegate)
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class IosImagePickerDelegate(
    private val onResult: (IosPickedImage) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            activeImagePickerDelegates.remove(this)
            return
        }

        val provider = result.itemProvider
        provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val picked = data?.toPickedImage(provider.suggestedName)
            dispatch_async(dispatch_get_main_queue()) {
                activeImagePickerDelegates.remove(this)
                if (picked != null) {
                    onResult(picked)
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toPickedImage(suggestedName: String?): IosPickedImage? {
    val length = length.toInt()
    if (length <= 0) return null
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this@toPickedImage.bytes, this@toPickedImage.length)
    }

    val originalName = suggestedName?.takeIf { it.isNotBlank() } ?: "avatar.jpg"
    val extension = originalName.substringAfterLast('.', "").lowercase()
    val mimeType = when (extension) {
        "png" -> "image/png"
        "gif" -> "image/gif"
        "heic", "heif" -> "image/heic"
        "webp" -> "image/webp"
        "tif", "tiff" -> "image/tiff"
        else -> "image/jpeg"
    }
    val filename = if ('.' in originalName) {
        originalName
    } else {
        "$originalName.${if (mimeType == "image/png") "png" else "jpg"}"
    }
    return IosPickedImage(bytes = bytes, filename = filename, mimeType = mimeType)
}
