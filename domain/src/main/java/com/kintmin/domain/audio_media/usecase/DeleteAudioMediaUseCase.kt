package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class DeleteAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase,
    private val log: Log,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        source: String,
    ): Result<Unit> = runCatching {
        // 삭제에 관여된 모든 플레이리스트 id를 가져온다.
        val targetPlaylistIdList = audioMediaRepository.deleteAudioMedia(audioMediaId).onSuccess {
            log.sendFirebaseEvent(FirebaseEvent.DeleteAudioMedia(source))
        }.getOrThrow()

        supervisorScope {
            // 변경된 플레이리스트는 전부 업데이트한다.
            targetPlaylistIdList.map { playlistId ->
                launch { updateOnPlaylistChangeUseCase(playlistId) }
            }.joinAll()
        }
    }
}