package com.kintmin.domain.usecase

import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.PlaybackRepository
import javax.inject.Inject

class AddAudioMediaListToPlaylistUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return runCatching {
            playbackRepository.addAudioMediaListToPlaylist(playlistId, audioMediaIdList).onSuccess {
                playbackRepository.deleteAudioMediaListInPlaylist(Playlist.UNCATEGORIZED, audioMediaIdList).getOrThrow()
                // 플레이리스트 음원수랑 재생시간 업데이트
            }.getOrThrow()
        }
    }
}