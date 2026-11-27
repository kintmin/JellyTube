package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

/** 가사를 지운다. 원본에서 파생된 편집본 시간축 가사도 함께 떨어져 나간다. */
class DeleteAudioMediaLyricsUseCase(
    private val audioMediaRepository: AudioMediaRepository,
    private val refreshEditedAudioLyricsUseCase: RefreshEditedAudioLyricsUseCase,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<Unit> =
        audioMediaRepository.deleteLyrics(audioMediaId)
            .onSuccess { refreshEditedAudioLyricsUseCase(audioMediaId) }
}
