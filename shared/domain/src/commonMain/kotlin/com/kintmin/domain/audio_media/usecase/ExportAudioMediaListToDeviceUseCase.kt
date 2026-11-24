package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.AudioMediaExportStatus
import com.kintmin.domain.audio_media.model.AudioMediaExportSummary
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.secret
import kotlinx.coroutines.flow.first

class ExportAudioMediaListToDeviceUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val audioTrackRepository: AudioTrackRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaList: List<AudioMedia>,
        targetList: List<AudioExportTarget>,
    ): Result<AudioMediaExportSummary> = suspendRunCatching {
        if (targetList.isEmpty()) throw Exception("저장할 용도를 하나 이상 선택해주세요.")

        val secretIds = audioTrackRepository.getSecretAudioMediaIdListFlow().first().toSet()
        var exportedCount = 0
        var alreadyExportedCount = 0
        val failedNameList = mutableListOf<String>()

        audioMediaList.forEach { audioMedia ->
            audioMediaRepository.exportAudioMediaToDevice(
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
                appLog.sendErrorLog(
                    ErrorLog("ExportAudioMediaListToDeviceUseCase", "${secret(audioMedia.name, audioMedia.id in secretIds)} 기기 저장 실패", throwable),
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
