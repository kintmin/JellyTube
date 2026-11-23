package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaListUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import kotlinx.coroutines.flow.first

/**
 * 플레이리스트를 지우면서 담긴 곡의 파일까지 지운다. 곡을 남기는 쪽은 [DeletePlaylistUseCase] 이고,
 * 그쪽은 다른 데 속하지 않은 곡을 미분류로 돌려보내 "전체" 에도 나타나게 한다.
 *
 * 곡을 먼저 지워야 한다. 플레이리스트가 먼저 사라지면 어떤 곡이 담겨 있었는지 알 수 없다.
 */
class DeletePlaylistWithTracksUseCase constructor(
    private val fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val deleteAudioMediaListUseCase: DeleteAudioMediaListUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
) {
    suspend operator fun invoke(playlistId: Int): Result<Unit> {
        val tracks = fetchAudioMediaListFlowUseCase(playlistId).first()
        return deleteAudioMediaListUseCase(
            idList = tracks.map { it.audioMedia.id },
            sourceList = tracks.map { it.audioMedia.source },
        ).mapCatching { deletePlaylistUseCase(playlistId).getOrThrow() }
    }
}
