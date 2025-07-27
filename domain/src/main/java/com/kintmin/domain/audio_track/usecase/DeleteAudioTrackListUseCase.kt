package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.usecase.AddUncategorizedPlaylistUseCase
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class DeleteAudioTrackListUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase,
    private val addUncategorizedPlaylistUseCase: AddUncategorizedPlaylistUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> = runCatching {
        audioTrackRepository.deleteAudioTrackList(playlistId, audioMediaIdList).getOrThrow()

        supervisorScope {
            listOf(
                launch { updateOnPlaylistChangeUseCase(playlistId) },
                launch { addUncategorizedPlaylistUseCase(audioMediaIdList) },
            ).joinAll()
        }
    }
}