package com.kintmin.presentation.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.net.toUri

/** DocumentsUI 의 "오디오" 루트에서 열리는 다중 선택 contract. MediaDocumentsProvider 가 제공한다. */
private class OpenMultipleAudioDocuments : ActivityResultContracts.OpenMultipleDocuments() {

    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, AUDIO_ROOT_URI)
        }

    private companion object {
        val AUDIO_ROOT_URI: Uri =
            "content://com.android.providers.media.documents/root/audio_root".toUri()
    }
}

private val AUDIO_MIME_TYPES = arrayOf("audio/*")

@Composable
actual fun rememberAudioFilePicker(
    startAtAudioRoot: Boolean,
    onPicked: (fileIdentifiers: List<String>) -> Unit,
): FilePickerLauncher {
    val contract = remember(startAtAudioRoot) {
        if (startAtAudioRoot) OpenMultipleAudioDocuments()
        else ActivityResultContracts.OpenMultipleDocuments()
    }
    val launcher = rememberLauncherForActivityResult(contract) { uris ->
        onPicked(uris.map { it.toString() })
    }
    return remember(launcher) {
        FilePickerLauncher { launcher.launch(AUDIO_MIME_TYPES) }
    }
}
