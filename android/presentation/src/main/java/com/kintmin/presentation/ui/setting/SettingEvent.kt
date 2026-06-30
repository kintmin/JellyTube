package com.kintmin.presentation.ui.setting

sealed interface SettingEvent {
    data object NavigateToStepScreen : SettingEvent
    data object NavigateToAppLogScreen : SettingEvent
    data object NavigateToShareScreen : SettingEvent
    data object NavigateToFileShareReceiveScreen : SettingEvent
    data object RequestActivityRecognitionPermission : SettingEvent
    data object StopStepForegroundService : SettingEvent
    data class ShowToast(val message: String) : SettingEvent
}
