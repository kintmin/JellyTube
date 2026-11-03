package com.kintmin.presentation.ui.setting.device_save_select

sealed interface SettingDeviceSaveSelectEvent {
    data class ShowToast(val message: String) : SettingDeviceSaveSelectEvent
}
