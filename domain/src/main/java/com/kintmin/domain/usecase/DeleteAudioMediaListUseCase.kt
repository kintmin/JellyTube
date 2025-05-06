package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistImageWhenUpdatePlaybackUseCase
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DeleteAudioMediaListUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdatePlaybackUseCase: UpdatePlaylistImageWhenUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, idList: List<Int>): Result<Unit> = runCatching {
        coroutineScope {
            idList.map { id ->
                async { audioMediaRepository.deleteAudioMedia(id).getOrThrow() }
            }.awaitAll()

            listOf(
                async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                async { updatePlaylistImageWhenUpdatePlaybackUseCase(playlistId) },
                async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                async { updatePlaylistImageWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                async { updatePlaylistImageWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
            ).awaitAll()
        }
    }
}