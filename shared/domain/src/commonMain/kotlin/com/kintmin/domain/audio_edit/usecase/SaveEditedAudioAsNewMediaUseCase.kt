package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.BlankEditedAudioName
import com.kintmin.domain.audio_edit.model.EmptyAudioEditSegment
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.BuildEditedAudioLyricsUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlinx.coroutines.flow.first

/**
 * 편집 결과를 원본과 별개인 새 음원으로 저장한다.
 * 새로 생긴 음원이 어느 플레이리스트로 들어갈지는 다운로드 설정을 그대로 따른다.
 *
 * 썸네일과 마찬가지로 가사도 물려준다 — 조각 배치에 맞춰 옮긴 가사가 새 곡의 정본이 된다.
 * 새 곡은 길이가 달라 가사를 다시 찾을 수 없으므로, 여기서 물려주지 않으면 손으로 넣을 수밖에 없다.
 */
class SaveEditedAudioAsNewMediaUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
    private val audioMediaRepository: AudioMediaRepository,
    private val buildEditedAudioLyricsUseCase: BuildEditedAudioLyricsUseCase,
    private val applyLyricsToAudioMediaUseCase: ApplyLyricsToAudioMediaUseCase,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        parts: List<AudioEditPart>,
        name: String,
    ): Result<AudioMedia> {
        audioEditRepository.withSavingMark(audioMediaId) {
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
            result.onSuccess { carryLyrics(originAudioMediaId = audioMediaId, parts = parts, newAudioMediaId = it.id) }
            return result
        }
    }

    /** 가사를 물려주지 못해도 곡은 남는다 — 썸네일과 같은 취급이다. */
    private suspend fun carryLyrics(originAudioMediaId: Int, parts: List<AudioEditPart>, newAudioMediaId: Int) {
        val originLyricFileFullPath = audioMediaRepository.getAudioMedia(originAudioMediaId)
            .getOrNull()?.lyricFileFullPath ?: return
        val built = buildEditedAudioLyricsUseCase(lyricFileFullPath = originLyricFileFullPath, parts = parts) ?: return
        applyLyricsToAudioMediaUseCase(
            audioMediaId = newAudioMediaId,
            plainLyrics = null,
            syncedLyrics = built.synced,
            translationLyrics = built.translation,
            transliterationLyrics = built.transliteration,
        )
    }
}
