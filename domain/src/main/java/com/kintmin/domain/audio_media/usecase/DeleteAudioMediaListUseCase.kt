package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteAudioMediaListUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
) {
    suspend operator fun invoke(idList: List<Int>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val mutex = Mutex()
            val targetPlaylistIdSet = mutableSetOf<Int>()

            supervisorScope {
                idList.map { id ->
                    launch {
                        val targetPlaylistIdList = audioMediaRepository.deleteAudioMedia(id).getOrThrow()
                        mutex.withLock {
                            targetPlaylistIdSet.addAll(targetPlaylistIdList)
                        }
                    }
                }.joinAll()
            }

            supervisorScope {
                targetPlaylistIdSet.flatMap  { playlistId ->
                    listOf(
                        launch { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                        launch { updatePlaylistImageWhenUpdateTrackUseCase(playlistId) },
                    )
                }.joinAll()
            }

            supervisorScope {
                listOf(
                    launch { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                    launch { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.TOTAL) },
                    launch { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                    launch { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.UNCATEGORIZED) },
                ).joinAll()
            }
        }
    }
}