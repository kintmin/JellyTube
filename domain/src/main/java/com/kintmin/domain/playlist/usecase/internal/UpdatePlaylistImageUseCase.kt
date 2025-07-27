package com.kintmin.domain.playlist.usecase.internal

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class UpdatePlaylistImageUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int): Result<Unit> {
        return runCatching {
            // 플레이리스트가 자체 이미지를 쓰고 있다면 종료
            val playlist = playlistRepository.getPlaylistFlow(playlistId)
                .flowOn(Dispatchers.IO)
                .first()
            if (playlist.isCustomImage) return@runCatching

            // 플레이리스트에 아무 미디어가 없다면 종료
            val firstData = audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId)
                .flowOn(Dispatchers.IO)
                .first()
                .minByOrNull {
                    it.audioTrack.trackSequence
                } ?: return@runCatching

            // 플레이리스트의 사진을 첫번째 미디어 사진으로 변경
            firstData.audioMedia.imageFileFullPath?.let {
                if (it == playlist.imageFileFullPath) return@runCatching
                playlistRepository.updatePlaylist(
                    id = playlistId,
                    imageFileFullPath = it,
                ).getOrThrow()
            }
        }
    }
}