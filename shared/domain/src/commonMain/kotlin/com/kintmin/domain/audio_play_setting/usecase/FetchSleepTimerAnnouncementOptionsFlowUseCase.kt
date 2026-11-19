package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.usecase.FetchAllAudioMediaListFlowUseCase
import com.kintmin.domain.audio_play_setting.model.SleepTimerAnnouncementOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FetchSleepTimerAnnouncementOptionsFlowUseCase(
    private val fetchSleepTimerAnnouncementMediaIdFlowUseCase: FetchSleepTimerAnnouncementMediaIdFlowUseCase,
    private val fetchAllAudioMediaListFlowUseCase: FetchAllAudioMediaListFlowUseCase,
) {
    operator fun invoke(): Flow<SleepTimerAnnouncementOptions> = combine(
        fetchSleepTimerAnnouncementMediaIdFlowUseCase(),
        fetchAllAudioMediaListFlowUseCase(),
        ::resolve,
    )

    /** 고른 음원이 지워졌거나 길이 제한을 넘기면 고르지 않은 것으로 떨어진다. */
    internal fun resolve(savedId: Int?, mediaList: List<AudioMedia>): SleepTimerAnnouncementOptions {
        val selectable = mediaList.filter { it.fitsSleepTimerAnnouncement }
        return SleepTimerAnnouncementOptions(
            selected = selectable.firstOrNull { it.id == savedId },
            selectable = selectable,
        )
    }
}
