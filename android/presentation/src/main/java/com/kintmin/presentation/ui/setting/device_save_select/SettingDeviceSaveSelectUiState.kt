package com.kintmin.presentation.ui.setting.device_save_select

import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.presentation.ui.setting.device_save_select.list.SettingDeviceSaveSelectListItemUiState

data class SettingDeviceSaveSelectUiState(
    val audioMediaList: List<SettingDeviceSaveSelectListItemUiState> = emptyList(),
    val checkedItemCount: Int = 0,
    val selectedTargetList: List<AudioExportTarget> = DEFAULT_TARGET_LIST,
    val isSaving: Boolean = false,
) {
    val isSaveEnabled: Boolean get() = !isSaving && checkedItemCount > 0 && selectedTargetList.isNotEmpty()

    companion object {

        val DEFAULT_TARGET_LIST = listOf(AudioExportTarget.MUSIC)

        fun getMock() = SettingDeviceSaveSelectUiState(
            audioMediaList = SettingDeviceSaveSelectListItemUiState.getMockList(),
            checkedItemCount = SettingDeviceSaveSelectListItemUiState.getMockList().count { it.isChecked },
        )
    }
}

/** 용도 선택 칩에 표시할 이름. */
internal val AudioExportTarget.displayName: String
    get() = when (this) {
        AudioExportTarget.MUSIC -> "음악"
        AudioExportTarget.ALARM -> "알람음"
        AudioExportTarget.NOTIFICATION -> "알림음"
        AudioExportTarget.RINGTONE -> "벨소리"
    }
