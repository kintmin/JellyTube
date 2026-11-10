package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import platform.Foundation.NSURL
import platform.UniformTypeIdentifiers.UTTypeAudio
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

/**
 * `asCopy = true` 로 연다 — 시스템이 고른 파일을 앱 임시 디렉터리로 먼저 복사해 주므로
 * 보안 스코프 URL 을 직접 다루지 않아도 되고, iCloud Drive 에만 있는 파일도 내려받아 준다.
 *
 * [startAtAudioRoot] 는 무시한다. iOS 문서 선택기에 "오디오 루트" 개념이 없다.
 */
@Composable
actual fun rememberAudioFilePicker(
    startAtAudioRoot: Boolean,
    onPicked: (fileIdentifiers: List<String>) -> Unit,
): FilePickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)

    // 델리게이트는 약참조로 잡힌다. 여기서 들고 있지 않으면 선택 직전에 사라진다.
    val delegate = remember {
        AudioDocumentPickerDelegate { urls -> currentOnPicked(urls) }
    }

    DisposableEffect(delegate) {
        onDispose { delegate.detach() }
    }

    return remember(delegate) {
        FilePickerLauncher {
            val presenter = topViewController() ?: return@FilePickerLauncher
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeAudio),
                asCopy = true,
            ).apply {
                allowsMultipleSelection = true
                this.delegate = delegate
            }
            delegate.attach(picker)
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class AudioDocumentPickerDelegate(
    private val onPicked: (List<String>) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    private var picker: UIDocumentPickerViewController? = null

    fun attach(picker: UIDocumentPickerViewController) {
        this.picker = picker
    }

    fun detach() {
        picker?.delegate = null
        picker = null
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        onPicked(didPickDocumentsAtURLs.filterIsInstance<NSURL>().mapNotNull { it.absoluteString })
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        // 호출부는 "고른 것이 없다"와 "취소"를 구분하지 않는다.
    }
}
