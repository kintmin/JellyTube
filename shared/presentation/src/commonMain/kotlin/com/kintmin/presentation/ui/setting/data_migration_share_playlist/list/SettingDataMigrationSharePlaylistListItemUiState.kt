package com.kintmin.presentation.ui.setting.data_migration_share_playlist.list

import com.kintmin.domain.data_migration.MigrationScopes
import com.kintmin.domain.playlist.model.Playlist

data class SettingDataMigrationSharePlaylistListItemUiState(
    val id: Int,
    val name: String,
    val audioMediaCount: Int,
    val isSecret: Boolean,
    val isChecked: Boolean,
    /** 받는 기기에서 가질 이름. [name] 과 같으면 따로 보여 주지 않는다. */
    val sharedName: String,
) {
    val subTitle: String get() = if (sharedName == name) {
        "${audioMediaCount}곡"
    } else {
        "${audioMediaCount}곡 · 받는 기기에서는 '$sharedName'"
    }

    companion object {
        fun getMock(index: Int = 1) = SettingDataMigrationSharePlaylistListItemUiState(
            id = index,
            name = "플레이리스트 $index",
            audioMediaCount = index * 3,
            isSecret = index == 3,
            isChecked = index % 2 == 0,
            sharedName = "플레이리스트 $index",
        )

        fun getMockList() = List(4) { index -> getMock(index) }
    }
}

internal fun Playlist.toSettingDataMigrationSharePlaylistListItemUiState(isChecked: Boolean) =
    SettingDataMigrationSharePlaylistListItemUiState(
        id = id,
        name = name,
        audioMediaCount = audioMediaCount,
        isSecret = type.isSecret,
        isChecked = isChecked,
        sharedName = MigrationScopes.sharedNameOf(type, name),
    )
