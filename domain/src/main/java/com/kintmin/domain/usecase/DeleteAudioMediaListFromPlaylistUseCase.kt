package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.AddUncategorizedPlaylistUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistAfterUpdatePlaybackUseCase
import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DeleteAudioMediaListFromPlaylistUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val updatePlaylistAfterUpdatePlaybackUseCase: UpdatePlaylistAfterUpdatePlaybackUseCase,
    private val addUncategorizedPlaylistUseCase: AddUncategorizedPlaylistUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> = runCatching {
        coroutineScope {
            audioMediaIdList.map { id ->
                async { playbackRepository.deletePlaylistTrack(playlistId, id).getOrThrow() }
            }.awaitAll()
            updatePlaylistAfterUpdatePlaybackUseCase(playlistId)
            addUncategorizedPlaylistUseCase(audioMediaIdList)
        }
    }
}