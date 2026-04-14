package com.kintmin.presentation.ui.setting

sealed interface SettingEvent {
    data object NavigateToAppLogScreen : SettingEvent
}
