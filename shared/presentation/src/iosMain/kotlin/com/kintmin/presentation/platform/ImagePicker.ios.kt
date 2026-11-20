package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/** PHPicker 는 앱 밖 프로세스에서 돌아 사진 접근 권한을 묻지 않는다. */
@Composable
actual fun rememberImagePicker(
    onPicked: (imageData: ByteArray) -> Unit,
): FilePickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)

    // 델리게이트는 약참조로 잡힌다. 여기서 들고 있지 않으면 선택 직전에 사라진다.
    val delegate = remember {
        ImagePickerDelegate { imageData -> currentOnPicked(imageData) }
    }

    DisposableEffect(delegate) {
        onDispose { delegate.detach() }
    }

    return remember(delegate) {
        FilePickerLauncher {
            val presenter = topViewController() ?: return@FilePickerLauncher
            val configuration = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration = configuration).apply {
                this.delegate = delegate
            }
            delegate.attach(picker)
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class ImagePickerDelegate(
    private val onPicked: (ByteArray) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    private var picker: PHPickerViewController? = null

    fun attach(picker: PHPickerViewController) {
        this.picker = picker
    }

    fun detach() {
        picker?.delegate = null
        picker = null
    }

    // 문서 선택기와 달리 PHPicker 는 스스로 닫히지 않는다.
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        val typeIdentifier = UTTypeImage.identifier
        result.itemProvider.loadDataRepresentationForTypeIdentifier(typeIdentifier) { data, _ ->
            val imageData = data?.toByteArray() ?: return@loadDataRepresentationForTypeIdentifier
            // 완료 콜백은 임의 스레드에서 온다.
            dispatch_async(dispatch_get_main_queue()) { onPicked(imageData) }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) usePinned { memcpy(it.addressOf(0), bytes, length) }
}
