package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistAfterUpdatePlaybackUseCase
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DeleteAudioMediaListUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistAfterUpdatePlaybackUseCase: UpdatePlaylistAfterUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, idList: List<Int>): Result<Unit> = runCatching {
        coroutineScope {
            idList.map { id ->
                async { audioMediaRepository.deleteAudioMedia(id).getOrThrow() }
            }.awaitAll()

            listOf(
                async { updatePlaylistAfterUpdatePlaybackUseCase(playlistId) },
                async { updatePlaylistAfterUpdatePlaybackUseCase(Playlist.TOTAL) },
                async { updatePlaylistAfterUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
            ).awaitAll()
        }
    }
}