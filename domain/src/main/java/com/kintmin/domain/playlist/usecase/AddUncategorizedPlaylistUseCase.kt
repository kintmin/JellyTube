package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AddUncategorizedPlaylistUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
) {
    suspend operator fun invoke(audioMediaIdList: List<Int>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val targetAudioMediaIdList = audioMediaIdList.map { audioMediaId ->
                async {
                    val playlistIdList = audioTrackRepository.getPlaylistIdListFlow(audioMediaId).first()
                    val isExistOnlyTotal = playlistIdList.size == 1
                    if (isExistOnlyTotal) audioMediaId else null
                }
            }.awaitAll().filterNotNull()

            audioTrackRepository.addAudioTrackList(Playlist.UNCATEGORIZED, targetAudioMediaIdList).getOrThrow()

            listOf(
                async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                async { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.UNCATEGORIZED) },
            ).awaitAll()
        }
    }
}