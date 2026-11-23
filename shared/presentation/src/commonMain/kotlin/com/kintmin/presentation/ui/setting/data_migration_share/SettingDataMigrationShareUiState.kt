package com.kintmin.presentation.ui.setting.data_migration_share

data class SettingDataMigrationShareUiState(
    val isSecretModeEnabled: Boolean = false,
    val secretPlaylistCount: Int = 0,
) {
    /** 비밀 플레이리스트가 없으면 안내를 띄우지 않는다 — 쓰지 않는 사람에게 기능을 드러내지 않는다. */
    val hasSecretPlaylist: Boolean get() = secretPlaylistCount > 0

    companion object {
        fun getMock() = SettingDataMigrationShareUiState(isSecretModeEnabled = true, secretPlaylistCount = 2)
    }
}
