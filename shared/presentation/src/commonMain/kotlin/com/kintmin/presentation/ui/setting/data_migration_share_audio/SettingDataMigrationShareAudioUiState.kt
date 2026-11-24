package com.kintmin.presentation.ui.setting.data_migration_share_audio

import com.kintmin.presentation.ui.setting.data_migration_share_audio.list.SettingDataMigrationShareAudioListItemUiState

data class SettingDataMigrationShareAudioUiState(
    val audioMediaList: List<SettingDataMigrationShareAudioListItemUiState> = emptyList(),
    val checkedAudioMediaIdList: List<Int> = emptyList(),
    /** [audioMediaList] 는 검색으로 걸러진 것이라 여기서 셀 수 없다. 전체선택은 검색과 무관하게 전부다. */
    val isAllChecked: Boolean = false,
) {
    val checkedCount: Int get() = checkedAudioMediaIdList.size
    val isSendEnabled: Boolean get() = checkedCount > 0

    companion object {
        fun getMock() = SettingDataMigrationShareAudioUiState(
            audioMediaList = SettingDataMigrationShareAudioListItemUiState.getMockList(),
            checkedAudioMediaIdList = SettingDataMigrationShareAudioListItemUiState.getMockList()
                .filter { it.isChecked }.map { it.id },
        )
    }
}
