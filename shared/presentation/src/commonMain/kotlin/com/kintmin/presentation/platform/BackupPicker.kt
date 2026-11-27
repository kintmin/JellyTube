package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/**
 * 백업을 둘 폴더를 고른다. `folderId` 는 앱을 다시 켜도 그 폴더를 열 수 있는 값이고 `label` 은 보여 줄 이름이다.
 * 재설치하면 그 값으로도 열 수 없어 사용자가 한 번 더 골라야 한다.
 */
@Composable
expect fun rememberBackupFolderPicker(onPicked: (folderId: String, label: String) -> Unit): FilePickerLauncher
