package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/**
 * 백업을 둘 폴더를 고른다. `folderId` 는 앱을 다시 켜도 그 폴더를 열 수 있는 값이고 `label` 은 보여 줄 이름이다.
 * 백업 자리가 정해진 플랫폼은 진입점을 숨기고 이 선택기는 아무것도 하지 않는다.
 */
@Composable
expect fun rememberBackupFolderPicker(onPicked: (folderId: String, label: String) -> Unit): FilePickerLauncher

/**
 * 재설치한 앱이 백업 폴더의 파일을 볼 수 없을 때 사용자가 그 파일을 직접 골라 준다.
 * 폴더를 다시 골라 읽을 수 있는 플랫폼은 진입점을 숨기고 이 선택기는 아무것도 하지 않는다.
 */
@Composable
expect fun rememberBackupFilePicker(onPicked: (fileId: String) -> Unit): FilePickerLauncher
