package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class AddUncategorizedPlaylistUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase,
) {
    suspend operator fun invoke(audioMediaIdList: List<Int>): Result<Unit> {
        return supervisorScope {
            runCatching {
                // 미분류에 추가될 id는 현재 전체에만 있는 미디어이기에 해당 id들만 추려낸다.
                val targetAudioMediaIdList = audioMediaIdList.map { audioMediaId ->
                    async(Dispatchers.IO) {
                        val playlistIdList = audioTrackRepository.getPlaylistIdListFlow(audioMediaId).first()
                        val isExistOnlyTotal = playlistIdList.size == 1
                        if (isExistOnlyTotal) audioMediaId else null
                    }
                }.awaitAll().filterNotNull()
                if (targetAudioMediaIdList.isEmpty()) return@runCatching

                audioTrackRepository.addAudioTrackList(Playlist.UNCATEGORIZED, targetAudioMediaIdList).getOrThrow()
                updateOnPlaylistChangeUseCase(Playlist.UNCATEGORIZED)
            }
        }
    }
}