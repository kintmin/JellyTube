package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.AddUncategorizedPlaylistUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistImageWhenUpdatePlaybackUseCase
import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DeleteAudioMediaListFromPlaylistUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdatePlaybackUseCase: UpdatePlaylistImageWhenUpdatePlaybackUseCase,
    private val addUncategorizedPlaylistUseCase: AddUncategorizedPlaylistUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> = runCatching {
        coroutineScope {
            audioMediaIdList.map { id ->
                async { playbackRepository.deletePlaylistTrack(playlistId, id).getOrThrow() }
            }.awaitAll()

            listOf(
                async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                async { updatePlaylistImageWhenUpdatePlaybackUseCase(playlistId) },
                async { addUncategorizedPlaylistUseCase(audioMediaIdList) },
            ).awaitAll()
        }
    }
}