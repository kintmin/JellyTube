package com.kintmin.domain.internal_usecase

import com.kintmin.domain.repository.AudioMediaRepository
import com.kintmin.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdatePlaylistAfterUpdatePlaybackUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(playlistId: Int) {
        val totalList = audioMediaRepository.getAudioMediaListFlow(playlistId).first()
        val currentCount = totalList.count()
        val currentPlayTimeDuration = totalList.sumOf { it.audioDuration?.inWholeSeconds ?: 0L }
        playlistRepository.updatePlaylistPlayback(playlistId, currentCount, currentPlayTimeDuration).getOrThrow()
    }
}