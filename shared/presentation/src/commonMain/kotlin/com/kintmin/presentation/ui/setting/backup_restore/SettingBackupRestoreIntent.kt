package com.kintmin.presentation.ui.setting.backup_restore

sealed interface SettingBackupRestoreIntent {
    data object OnClickBackupNow : SettingBackupRestoreIntent

    /** 폴더를 바꾸거나, 재설치 뒤 백업이 있는 폴더를 다시 고른다. */
    data object OnClickPickFolder : SettingBackupRestoreIntent
    data class OnBackupFolderPicked(val folderId: String, val label: String) : SettingBackupRestoreIntent

    /** 공용 폴더에 쓸 권한을 받았다. 받기 전에는 백업을 쓰지 못했다. */
    data object OnBackupStorageGranted : SettingBackupRestoreIntent

    /** 재설치한 앱이 백업 폴더의 파일을 볼 수 없어 사용자가 직접 찾는다. */
    data object OnClickFindBackupFile : SettingBackupRestoreIntent
    data class OnBackupFilePicked(val fileId: String) : SettingBackupRestoreIntent
    data object OnClickStartRestore : SettingBackupRestoreIntent

    /** Wi-Fi 가 아닌데도 복원하겠다고 했다. */
    data object OnConfirmCellularRestore : SettingBackupRestoreIntent
    data object OnDismissCellularRestore : SettingBackupRestoreIntent
    data object OnClickCancelPreview : SettingBackupRestoreIntent
    data object OnClickStop : SettingBackupRestoreIntent
    data object OnClickRetry : SettingBackupRestoreIntent
    data object OnClickCloseResult : SettingBackupRestoreIntent

    /** 나가기 확인 다이얼로그에서 나가기를 골랐다. */
    data object OnConfirmExit : SettingBackupRestoreIntent
}
