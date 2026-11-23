package com.kintmin.presentation.ui.setting.data_migration_share_audio.list

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class SettingDataMigrationShareAudioListItemUiState(
    val id: Int,
    val mediaName: String,
    val artist: String,
    val audioDuration: Duration?,
    val imageFileFullPath: String?,
    val isChecked: Boolean,
) {
    val subTitle: String get() = "$artist | ${audioDuration?.to_hh_colon_mm_colon_ss() ?: "알 수 없음"}"

    companion object {
        fun getMock(index: Int = 1) = SettingDataMigrationShareAudioListItemUiState(
            id = index,
            mediaName = "미디어 $index",
            artist = "아티스트",
            audioDuration = 500.seconds,
            imageFileFullPath = null,
            isChecked = index % 2 == 0,
        )

        fun getMockList() = List(5) { index -> getMock(index) }
    }
}

internal fun AudioMedia.toSettingDataMigrationShareAudioListItemUiState(isChecked: Boolean) =
    SettingDataMigrationShareAudioListItemUiState(
        id = id,
        mediaName = name,
        artist = artist,
        audioDuration = playbackDuration,
        imageFileFullPath = imageFileFullPath,
        isChecked = isChecked,
    )
