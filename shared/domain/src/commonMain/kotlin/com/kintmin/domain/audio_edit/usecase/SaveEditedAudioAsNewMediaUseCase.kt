package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlinx.coroutines.flow.first

/**
 * 편집 결과를 원본과 별개인 새 음원으로 저장한다.
 * 새로 생긴 음원이 어느 플레이리스트로 들어갈지는 다운로드 설정을 그대로 따른다.
 */
class SaveEditedAudioAsNewMediaUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        parts: List<AudioEditPart>,
        name: String,
    ): Result<AudioMedia> {
        if (parts.none { it is AudioEditPart.Audio }) {
            return Result.failure(EmptyAudioEditSegment())
        }
        if (name.isBlank()) {
            return Result.failure(BlankEditedAudioName())
        }

        val result = audioEditRepository.saveEditedAudioAsNewMedia(
            audioMediaId = audioMediaId,
            parts = parts,
            name = name.trim(),
            playlistIdOnDownload = fetchPlaylistIdOnDownloadFlowUseCase().first(),
            shouldInsertAtTopOnDownload = fetchShouldInsertAtTopOnDownloadFlowUseCase().first(),
        )

        appLog.sendDebugLog(
            DebugLog(
                tag = "SaveEditedAudioAsNewMediaUseCase",
                message = "편집본을 새 음원으로 저장: 원본 id $audioMediaId, 조각 ${parts.size}개, " +
                    "결과 ${result.exceptionOrNull() ?: "성공"}",
            )
        )
        return result
    }
}

class BlankEditedAudioName(
    override val message: String = "저장할 음원 이름을 입력해주세요."
) : Throwable()
