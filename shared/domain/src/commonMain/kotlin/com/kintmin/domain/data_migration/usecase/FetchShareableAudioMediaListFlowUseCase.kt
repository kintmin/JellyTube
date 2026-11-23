package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 곡 단위로 보낼 수 있는 것 — 전체에 보이는 곡이다.
 *
 * 비밀 곡은 비밀모드를 켜도 전체에 나오지 않으므로 여기에도 없다. 비밀 곡은 플레이리스트째로
 * 보낸다.
 */
class FetchShareableAudioMediaListFlowUseCase constructor(
    private val fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase,
    private val fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<AudioMedia>> = fetchPlaylistsByTypeUseCase(PlaylistType.TOTAL)
        .map { it.firstOrNull()?.id }
        .distinctUntilChanged()
        .flatMapLatest { totalPlaylistId ->
            if (totalPlaylistId == null) return@flatMapLatest flowOf(emptyList())
            fetchAudioMediaListFlowUseCase(totalPlaylistId).map { list -> list.map { it.audioMedia } }
        }
}
