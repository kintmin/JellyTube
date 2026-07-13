package com.kintmin.data.ios

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class IosFetchAudioMediaListFlowUseCaseBridge : KoinComponent {

    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> =
        get<FetchAudioMediaListFlowUseCase>()(playlistId)
}

fun createIosFetchAudioMediaListFlowUseCaseBridge(): IosFetchAudioMediaListFlowUseCaseBridge =
    IosFetchAudioMediaListFlowUseCaseBridge()

// Swift 측에서 kotlin.time.Duration? 접근이 SKIE 매핑 상 번거로우므로 초 단위 Long? 헬퍼를 노출한다.
fun audioMediaAudioDurationSeconds(audioMedia: AudioMedia): Long? =
    audioMedia.audioDuration?.inWholeSeconds
