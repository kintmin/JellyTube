package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

/**
 * 편집본 시간축 가사를 원본 가사와 편집 배치에서 다시 만든다. 둘 중 하나라도 없으면 떼어 낸다.
 *
 * 편집본 가사는 원본에서 파생되는 것이므로, 원본 가사나 편집 배치가 바뀐 자리마다 이것을 부른다.
 * 멱등이다. **실패를 호출자에게 올리지 않는다** — 원본 가사와 편집본 음원은 그대로 남고 편집본
 * 가사만 비는데, 그때는 원본 가사가 흐르지 않는 상태로 보인다.
 */
class RefreshEditedAudioLyricsUseCase(
    private val audioMediaRepository: AudioMediaRepository,
    private val audioEditRepository: AudioEditRepository,
    private val buildEditedAudioLyricsUseCase: BuildEditedAudioLyricsUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(audioMediaId: Int) {
        suspendRunCatching {
            val lyricFileFullPath = audioMediaRepository.getAudioMedia(audioMediaId).getOrThrow().lyricFileFullPath
            val parts = audioEditRepository.fetchEditTimeline(audioMediaId).getOrThrow()?.toParts()
            val built = if (parts == null || lyricFileFullPath == null) {
                null
            } else {
                buildEditedAudioLyricsUseCase(lyricFileFullPath = lyricFileFullPath, parts = parts)
            }

            if (built == null) {
                audioMediaRepository.clearEditedLyrics(audioMediaId).getOrThrow()
                return@suspendRunCatching
            }

            val editedPath = audioMediaRepository.saveLyrics(built.synced, synced = true).getOrThrow()
            built.translation?.let {
                audioMediaRepository.saveVariantLyrics(editedPath, LyricsVariant.TRANSLATION, it).getOrThrow()
            }
            built.transliteration?.let {
                audioMediaRepository.saveVariantLyrics(editedPath, LyricsVariant.TRANSLITERATION, it).getOrThrow()
            }
            audioMediaRepository.applyEditedLyrics(id = audioMediaId, lyricFileFullPath = editedPath).getOrThrow()
        }.onFailure {
            appLog.sendErrorLog(ErrorLog(LOG_TAG, "편집본 가사 만들기 실패: id=$audioMediaId", it))
        }
    }
}

private const val LOG_TAG = "RefreshEditedAudioLyrics"
