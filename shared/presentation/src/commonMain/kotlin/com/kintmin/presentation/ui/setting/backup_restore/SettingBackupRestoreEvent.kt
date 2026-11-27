package com.kintmin.presentation.ui.setting.backup_restore

sealed interface SettingBackupRestoreEvent {
    data object NavigateToBack : SettingBackupRestoreEvent
    data object LaunchBackupFolderPicker : SettingBackupRestoreEvent
    data class ShowToast(val message: String) : SettingBackupRestoreEvent
}
