package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.lyrics.model.LyricsVariant

/**
 * 선택한 가사를 현재 음원에 적용한다.
 * 저장은 항상 SYNC(LRC) 로 통일한다. syncedLyrics(LRC)가 있으면 그대로 저장하고,
 * 없으면 plainLyrics 의 각 줄에 [00:00.00] 태그를 붙여 SYNC 가사로 변환해 저장한 뒤
 * 음원의 lyricFileFullPath 컬럼을 갱신한다.
 *
 * 번역/음차 변형 가사가 함께 넘어오면 새 원본 파일명에서 파생한 별도 파일로 각각 저장한다.
 * 원본 파일명이 바뀌면 옛 변형 파일은 orphan 이 되므로 정리한다.
 */
class ApplyLyricsToAudioMediaUseCase(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        plainLyrics: String?,
        syncedLyrics: String?,
        translationLyrics: String? = null,
        transliterationLyrics: String? = null,
        previousLyricFileFullPath: String? = null,
    ): Result<Unit> = runCatching {
        val text = when {
            !syncedLyrics.isNullOrBlank() -> syncedLyrics
            !plainLyrics.isNullOrBlank() -> plainLyrics.lines().joinToString("\n") { "[00:00.00]$it" }
            else -> error("적용할 가사가 없습니다.")
        }
        val lyricFileFullPath = audioMediaRepository.saveLyrics(text, synced = true).getOrThrow()

        // 번역/음차도 새 원본 파일명 기준으로 각각 별도 파일에 저장한다.
        translationLyrics?.let {
            audioMediaRepository.saveVariantLyrics(lyricFileFullPath, LyricsVariant.TRANSLATION, it).getOrThrow()
        }
        transliterationLyrics?.let {
            audioMediaRepository.saveVariantLyrics(lyricFileFullPath, LyricsVariant.TRANSLITERATION, it).getOrThrow()
        }

        audioMediaRepository.updateAudioMedia(
            id = audioMediaId,
            lyricFileFullPath = lyricFileFullPath,
        ).getOrThrow()

        // 원본 파일명이 바뀐 경우 옛 변형 파일을 정리한다. 삭제 실패는 무시한다.
        previousLyricFileFullPath?.takeIf { it != lyricFileFullPath }?.let { old ->
            audioMediaRepository.deleteVariantLyrics(old, LyricsVariant.TRANSLATION)
            audioMediaRepository.deleteVariantLyrics(old, LyricsVariant.TRANSLITERATION)
        }
    }
}
