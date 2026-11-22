package com.kintmin.presentation.ui.setting

import com.kintmin.domain.app_update.model.AppRelease

sealed interface SettingEvent {
    data object NavigateToStepScreen : SettingEvent
    data object NavigateToAppLogScreen : SettingEvent
    data object NavigateToBackupRestoreScreen : SettingEvent
    data object LaunchBackupFolderPicker : SettingEvent
    data object LaunchAudioFilePicker : SettingEvent
    data object NavigateToDeviceSaveScreen : SettingEvent
    data object NavigateToSecretPlaylistGuideScreen : SettingEvent
    data object NavigateToFileShareReceiveScreen : SettingEvent
    data object NavigateToDataMigrationSendScreen : SettingEvent
    data object NavigateToDataMigrationReceiveScreen : SettingEvent
    data object RequestActivityRecognitionPermission : SettingEvent
    data class ShowToast(val message: String) : SettingEvent
    data class CopyDeviceId(val deviceId: String) : SettingEvent
    data class ShowAppUpdateDialog(val release: AppRelease) : SettingEvent
}
