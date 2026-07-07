package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

/**
 * 선택한 가사를 현재 음원에 적용한다.
 * syncedLyrics(LRC)가 있으면 싱크 가사로, 없으면 plainLyrics를 일반 가사로 파일 저장한 뒤
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
        val synced = !syncedLyrics.isNullOrBlank()
        val text = if (synced) syncedLyrics!! else (plainLyrics ?: error("적용할 가사가 없습니다."))
        val lyricFileFullPath = audioMediaRepository.saveLyrics(text, synced).getOrThrow()
        audioMediaRepository.updateAudioMedia(
            id = audioMediaId,
            lyricFileFullPath = lyricFileFullPath,
        ).getOrThrow()
    }
}
