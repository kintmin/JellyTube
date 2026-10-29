package com.kintmin.presentation.ui.setting.device_save_select

import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.presentation.ui.setting.device_save_select.list.SettingDeviceSaveSelectListItemUiState

sealed interface SettingDeviceSaveSelectIntent {
    data class OnChangeSearchText(val searchText: String) : SettingDeviceSaveSelectIntent
    data class OnClickAudioItem(val data: SettingDeviceSaveSelectListItemUiState) : SettingDeviceSaveSelectIntent
    data class OnToggleExportTarget(val target: AudioExportTarget) : SettingDeviceSaveSelectIntent
    data object OnClickClearCheckedItemList : SettingDeviceSaveSelectIntent
    data object OnClickSaveToDevice : SettingDeviceSaveSelectIntent
}
