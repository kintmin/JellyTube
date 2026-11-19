package com.kintmin.presentation.ui.setting.device_save_select

sealed interface SettingDeviceSaveSelectEvent {
    data class ShowToast(val message: String) : SettingDeviceSaveSelectEvent
    data class ShowRingtoneTooLong(val audioMediaId: Int) : SettingDeviceSaveSelectEvent
    data class ConfirmRingtoneShare(
        val fileFullPath: String,
        val title: String,
        val artist: String,
    ) : SettingDeviceSaveSelectEvent
}
