package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.FirebaseEvent

/**
 * 일부 성공/삭제가 발생할 수 있다.
 */
class DeleteAudioMediaListUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(idList: List<Int>, sourceList: List<String>): Result<Unit> = runCatching {
        idList.mapIndexed { index, id ->
            audioMediaRepository.deleteAudioMedia(id).getOrThrow()
            appLog.sendFirebaseEvent(FirebaseEvent.DeleteAudioMedia(sourceList[index]))
        }
    }
}
