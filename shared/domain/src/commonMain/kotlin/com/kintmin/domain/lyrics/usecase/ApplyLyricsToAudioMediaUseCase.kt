package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

/**
 * 선택한 가사를 현재 음원에 적용한다.
 * 저장은 항상 SYNC(LRC) 로 통일한다. syncedLyrics(LRC)가 있으면 그대로 저장하고,
 * 없으면 plainLyrics 의 각 줄에 [00:00.00] 태그를 붙여 SYNC 가사로 변환해 저장한 뒤
 * 음원의 lyricFileFullPath 컬럼을 갱신한다.
 */
class ApplyLyricsToAudioMediaUseCase(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        plainLyrics: String?,
        syncedLyrics: String?,
    ): Result<Unit> = runCatching {
        val text = when {
            !syncedLyrics.isNullOrBlank() -> syncedLyrics
            !plainLyrics.isNullOrBlank() -> plainLyrics.lines().joinToString("\n") { "[00:00.00]$it" }
            else -> error("적용할 가사가 없습니다.")
        }
        val lyricFileFullPath = audioMediaRepository.saveLyrics(text, synced = true).getOrThrow()
        audioMediaRepository.updateAudioMedia(
            id = audioMediaId,
            lyricFileFullPath = lyricFileFullPath,
        ).getOrThrow()
    }
}
