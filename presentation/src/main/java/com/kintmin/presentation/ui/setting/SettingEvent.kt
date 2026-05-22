package com.kintmin.presentation.ui.setting

sealed interface SettingEvent {
    data object NavigateToStepScreen : SettingEvent
    data object NavigateToAppLogScreen : SettingEvent
    data object RequestActivityRecognitionPermission : SettingEvent
}
