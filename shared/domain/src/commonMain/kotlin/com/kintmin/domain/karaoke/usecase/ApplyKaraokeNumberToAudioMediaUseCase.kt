package com.kintmin.domain.karaoke.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

/**
 * 선택한 노래방 번호를 현재 음원에 연동한다.
 */
class ApplyKaraokeNumberToAudioMediaUseCase(
    private val audioMediaRepository: AudioMediaRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(audioMediaId: Int, tjKaraokeNumber: String): Result<Unit> =
        audioMediaRepository.updateKaraokeNumber(audioMediaId, tjKaraokeNumber)
            .onFailure { appLog.sendErrorLog(ErrorLog("ApplyKaraokeNumber", "노래방 번호 적용 실패: id=$audioMediaId", it)) }
}
