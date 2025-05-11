package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.usecase.AddUncategorizedPlaylistUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteAudioTrackListUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
    private val addUncategorizedPlaylistUseCase: AddUncategorizedPlaylistUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            audioTrackRepository.deleteAudioTrackList(playlistId, audioMediaIdList).getOrThrow()

            listOf(
                async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                async { updatePlaylistImageWhenUpdateTrackUseCase(playlistId) },
                async { addUncategorizedPlaylistUseCase(audioMediaIdList) },
            ).awaitAll()
        }
    }
}