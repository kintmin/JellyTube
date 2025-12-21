package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 일부 성공/삭제가 발생할 수 있다.
 */
class DeleteAudioMediaListUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val log: Log,
) {

    suspend operator fun invoke(idList: List<Int>, sourceList: List<String>): Result<Unit> = runCatching {
        idList.mapIndexed { index, id ->
            audioMediaRepository.deleteAudioMedia(id).getOrThrow()
            log.sendFirebaseEvent(FirebaseEvent.DeleteAudioMedia(sourceList[index]))
        }
    }
}