package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.AudioMediaExportStatus
import com.kintmin.domain.audio_media.model.AudioMediaExportSummary
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog

/**
 * 선택한 음원들을 기기 공용 저장소로 내보낸다.
 *
 * 곡 단위 실패가 정상 결과(일부만 성공)라서 실패를 예외로 올리지 않고 집계해서 돌려준다.
 * 같은 디렉토리에서 파일명이 경합하지 않도록 순차로 처리한다.
 */
class ExportAudioMediaListToDeviceUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaList: List<AudioMedia>,
        targetList: List<AudioExportTarget>,
    ): Result<AudioMediaExportSummary> = runCatching {
        if (targetList.isEmpty()) throw Exception("저장할 용도를 하나 이상 선택해주세요.")

        var exportedCount = 0
        var alreadyExportedCount = 0
        val failedNameList = mutableListOf<String>()

        audioMediaList.forEach { audioMedia ->
            audioMediaRepository.exportAudioMediaToDevice(
                audioFileFullPath = audioMedia.audioFileFullPath,
                title = audioMedia.name,
                artist = audioMedia.artist,
                targetList = targetList,
            ).onSuccess { status ->
                when (status) {
                    AudioMediaExportStatus.EXPORTED -> exportedCount++
                    AudioMediaExportStatus.ALREADY_EXPORTED -> alreadyExportedCount++
                }
            }.onFailure { throwable ->
                failedNameList += audioMedia.name
                appLog.sendDebugLog(
                    DebugLog(
                        tag = "ExportAudioMediaListToDeviceUseCase",
                        message = "${audioMedia.name} 기기 저장 실패: $throwable",
                    )
                )
            }
        }

        AudioMediaExportSummary(
            exportedCount = exportedCount,
            alreadyExportedCount = alreadyExportedCount,
            failedNameList = failedNameList,
        )
    }
}
