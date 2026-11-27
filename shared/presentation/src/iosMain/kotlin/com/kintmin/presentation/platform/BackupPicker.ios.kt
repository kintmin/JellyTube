package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.base64EncodedStringWithOptions
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.darwin.NSObject

/** 폴더는 복사할 수 없어 원본 URL 을 받고, 앱을 다시 켜도 열리도록 security-scoped bookmark 로 남긴다. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberBackupFolderPicker(onPicked: (folderId: String, label: String) -> Unit): FilePickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    return rememberFolderPicker { url ->
        if (!url.startAccessingSecurityScopedResource()) return@rememberFolderPicker
        try {
            val bookmark = url.bookmarkDataWithOptions(0u, includingResourceValuesForKeys = null, relativeToURL = null, error = null)
                ?.base64EncodedStringWithOptions(0u)
                ?: return@rememberFolderPicker
            currentOnPicked(bookmark, url.lastPathComponent.orEmpty())
        } finally {
            url.stopAccessingSecurityScopedResource()
        }
    }
}

@Composable
private fun rememberFolderPicker(onPicked: (NSURL) -> Unit): FilePickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)

    // 델리게이트는 약참조로 잡힌다. 여기서 들고 있지 않으면 선택 직전에 사라진다.
    val delegate = remember { SingleDocumentPickerDelegate { url -> currentOnPicked(url) } }

    DisposableEffect(delegate) {
        onDispose { delegate.detach() }
    }

    return remember(delegate) {
        FilePickerLauncher {
            val presenter = topViewController() ?: return@FilePickerLauncher
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOfNotNull(UTTypeFolder),
                asCopy = false,
            ).apply {
                allowsMultipleSelection = false
                this.delegate = delegate
            }
            delegate.attach(picker)
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class SingleDocumentPickerDelegate(
    private val onPicked: (NSURL) -> Unit,
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
        didPickDocumentsAtURLs.filterIsInstance<NSURL>().firstOrNull()?.let(onPicked)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        // 호출부는 "고른 것이 없다"와 "취소"를 구분하지 않는다.
    }
}
