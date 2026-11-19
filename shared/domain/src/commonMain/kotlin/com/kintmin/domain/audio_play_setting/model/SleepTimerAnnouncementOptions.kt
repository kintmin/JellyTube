package com.kintmin.domain.audio_play_setting.model

import com.kintmin.domain.audio_media.model.AudioMedia

/** 잠들기 타이머의 마무리 음성으로 고를 수 있는 음원과, 그중 지금 고른 것. */
data class SleepTimerAnnouncementOptions(
    val selected: AudioMedia? = null,
    val selectable: List<AudioMedia> = emptyList(),
)
