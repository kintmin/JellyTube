package com.kintmin.domain.audio_play_setting.model

import com.kintmin.domain.audio_media.model.AudioMedia

data class SleepTimerAnnouncementOptions(
    val selected: AudioMedia? = null,
    val selectable: List<AudioMedia> = emptyList(),
)
