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
        /**
         * 기본 저장 용도.
         *
         * 용도 하나가 복사본 하나라서 전부 켜두면 디스크를 4배로 쓴다. 알람음으로 쓰려는 사용자가
         * 많고 음악 앱 노출은 기본적으로 기대되는 동작이라 이 둘만 기본으로 켠다.
         */
        val DEFAULT_TARGET_LIST = listOf(AudioExportTarget.ALARM, AudioExportTarget.MUSIC)

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
