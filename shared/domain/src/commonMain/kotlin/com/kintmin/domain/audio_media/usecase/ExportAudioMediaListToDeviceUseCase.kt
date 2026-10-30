package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.AudioMediaExportStatus
import com.kintmin.domain.audio_media.model.AudioMediaExportSummary
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog

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
                // 편집본이 있으면 편집본을 내보낸다
                audioFileFullPath = audioMedia.playbackFileFullPath,
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
