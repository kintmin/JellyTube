package com.kintmin.presentation.platform

import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBackupFolderPicker(onPicked: (folderId: String, label: String) -> Unit): FilePickerLauncher {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        val uri = treeUri ?: return@rememberLauncherForActivityResult
        // 영구 권한을 받아 두지 않으면 다음 실행에서 이 폴더를 열 수 없다. 제공자가 주지 못하거나
        // 기기의 영구 권한 수가 찼으면 던지는데, 그때 등록하면 다음에 열지 못하는 폴더가 남는다.
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.isSuccess
        if (!persisted) return@rememberLauncherForActivityResult
        currentOnPicked(uri.toString(), treeFolderLabelOf(DocumentsContract.getTreeDocumentId(uri)))
    }
    return remember(launcher) {
        // 선택기를 백업을 두기 좋은 자리에서 연다. 사용자가 다른 폴더를 골라도 된다.
        val initialFolder = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_PROVIDER, INITIAL_FOLDER_DOCUMENT_ID)
        FilePickerLauncher { launcher.launch(initialFolder) }
    }
}

/** `primary:Download/JellyTube` 처럼 저장소 이름이 콜론 앞에 붙어 온다. 사용자에게는 뒤쪽만 보인다. */
internal fun treeFolderLabelOf(treeDocumentId: String): String =
    treeDocumentId.substringAfter(':').ifEmpty { treeDocumentId }

private const val EXTERNAL_STORAGE_PROVIDER = "com.android.externalstorage.documents"
private const val INITIAL_FOLDER_DOCUMENT_ID = "primary:Download/JellyTube"
