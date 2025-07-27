package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class DeleteAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<Unit> = runCatching {
        // 삭제에 관여된 모든 플레이리스트 id를 가져온다.
        val targetPlaylistIdList = audioMediaRepository.deleteAudioMedia(audioMediaId).getOrThrow()

        // 변경된 플레이리스트는 전부 업데이트한다.
        supervisorScope {
            targetPlaylistIdList.map { playlistId ->
                launch { updateOnPlaylistChangeUseCase(playlistId) }
            }.joinAll()
        }
    }
}