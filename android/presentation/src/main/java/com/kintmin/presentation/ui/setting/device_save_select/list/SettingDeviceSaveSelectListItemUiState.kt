package com.kintmin.presentation.ui.setting.device_save_select.list

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class SettingDeviceSaveSelectListItemUiState(
    val id: Int,
    val mediaName: String,
    val artist: String,
    val audioDuration: Duration?,
    val imageFileFullPath: String?,
    val isChecked: Boolean,
) {
    val subTitle: String get() = "$artist | $audioDurationString"

    private val audioDurationString: String
        get() = audioDuration?.to_hh_colon_mm_colon_ss() ?: "알 수 없음"

    companion object {
        fun getMock(index: Int = 1) = SettingDeviceSaveSelectListItemUiState(
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

internal fun AudioMedia.toSettingDeviceSaveSelectListItemUiState(isChecked: Boolean) =
    SettingDeviceSaveSelectListItemUiState(
        id = id,
        mediaName = name,
        artist = artist,
        // 기기 저장은 편집본을 내보내므로 길이도 편집본 기준으로 보여준다
        audioDuration = playbackDuration,
        imageFileFullPath = imageFileFullPath,
        isChecked = isChecked,
    )
