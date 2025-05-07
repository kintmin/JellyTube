package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.AddUncategorizedPlaylistUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistImageWhenUpdatePlaybackUseCase
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.AudioMediaRepository
import com.kintmin.domain.repository.PlaylistRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val addUncategorizedPlaylistUseCase: AddUncategorizedPlaylistUseCase,
) {

    suspend operator fun invoke(playlistId: Int) {
        runCatching {
            coroutineScope {
                val audioMediaIdList = audioMediaRepository.getAudioMediaListFlow(playlistId).map { ausioMediaList ->
                    ausioMediaList.map { it.id }
                }.first()
                playlistRepository.deletePlaylist(playlistId).getOrThrow()
                addUncategorizedPlaylistUseCase(audioMediaIdList)
            }
        }
    }
}