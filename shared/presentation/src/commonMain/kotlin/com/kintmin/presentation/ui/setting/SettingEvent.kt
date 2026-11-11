package com.kintmin.presentation.ui.setting

sealed interface SettingEvent {
    data object NavigateToStepScreen : SettingEvent
    data object NavigateToAppLogScreen : SettingEvent
    data object LaunchAudioFilePicker : SettingEvent
    data object NavigateToDeviceSaveScreen : SettingEvent
    data object NavigateToShareScreen : SettingEvent
    data object NavigateToFileShareReceiveScreen : SettingEvent
    data object NavigateToDataMigrationSendScreen : SettingEvent
    data object NavigateToDataMigrationReceiveScreen : SettingEvent
    data object RequestActivityRecognitionPermission : SettingEvent
    data class ShowToast(val message: String) : SettingEvent
}
