package com.kintmin.presentation.platform

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** 백업은 공용 다운로드 폴더에 정해져 있어 고를 폴더가 없다. */
@Composable
actual fun rememberBackupFolderPicker(onPicked: (folderId: String, label: String) -> Unit): FilePickerLauncher =
    remember { FilePickerLauncher {} }

@Composable
actual fun rememberBackupFilePicker(onPicked: (fileId: String) -> Unit): FilePickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(OpenInBackupFolder()) { uri ->
        uri?.let { currentOnPicked(it.toString()) }
    }
    // 제공자마다 .gz 의 MIME 이 달라 거르지 않는다.
    return remember(launcher) { FilePickerLauncher { launcher.launch(arrayOf("*/*")) } }
}

/** 선택기를 백업 파일이 있는 폴더에서 연다. 그 폴더가 없으면 선택기가 기본 위치에서 연다. */
private class OpenInBackupFolder : ActivityResultContracts.OpenDocument() {

    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).apply {
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download/JellyTube"),
            )
        }
}
