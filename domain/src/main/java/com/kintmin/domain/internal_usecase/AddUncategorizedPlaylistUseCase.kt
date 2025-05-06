package com.kintmin.domain.internal_usecase

import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class AddUncategorizedPlaylistUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdatePlaybackUseCase: UpdatePlaylistImageWhenUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(audioMediaIdList: List<Int>): Result<Unit> = runCatching {
        coroutineScope {
            val targetAudioMediaIdList = audioMediaIdList.map { audioMediaId ->
                async {
                    val playlistIdList = playbackRepository.getPlaylistIdList(audioMediaId).getOrThrow()
                    val isExistOnlyTotal = playlistIdList.size == 1
                    if (isExistOnlyTotal) audioMediaId else null
                }
            }.awaitAll().filterNotNull()

            playbackRepository.addAudioMediaListToPlaylist(Playlist.UNCATEGORIZED, targetAudioMediaIdList).getOrThrow()

            listOf(
                async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                async { updatePlaylistImageWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
            ).awaitAll()
        }
    }
}