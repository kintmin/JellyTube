package com.kintmin.presentation.ui.setting.backup_restore

sealed interface SettingBackupRestoreIntent {
    data object OnClickBackupNow : SettingBackupRestoreIntent

    /** 폴더를 바꾸거나, 재설치 뒤 백업이 있는 폴더를 다시 고른다. */
    data object OnClickPickFolder : SettingBackupRestoreIntent
    data class OnBackupFolderPicked(val folderId: String, val label: String) : SettingBackupRestoreIntent
    data object OnClickStartRestore : SettingBackupRestoreIntent

    /** Wi-Fi 가 아닌데도 복원하겠다고 했다. */
    data object OnConfirmCellularRestore : SettingBackupRestoreIntent
    data object OnDismissCellularRestore : SettingBackupRestoreIntent
    data object OnClickStop : SettingBackupRestoreIntent
    data object OnClickRetry : SettingBackupRestoreIntent
    data object OnClickCloseResult : SettingBackupRestoreIntent

    /** 나가기 확인 다이얼로그에서 나가기를 골랐다. */
    data object OnConfirmExit : SettingBackupRestoreIntent
}
