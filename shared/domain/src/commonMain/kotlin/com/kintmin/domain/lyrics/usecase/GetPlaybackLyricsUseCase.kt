package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.lyrics.model.LyricsLine

/**
 * 재생에 맞춰 보여 줄 가사를 읽는다. 가사가 없으면 빈 목록이다.
 *
 * **편집본이 있으면 sync 를 떼어 글로만 보여 준다.** 가사의 시각은 원본 음원 기준인데 편집본은
 * 구간을 떼어 이어 붙인 것이라 소리와 어긋난다. 틀린 줄을 짚는 것보다 짚지 않는 쪽이 낫다.
 */
class GetPlaybackLyricsUseCase(
    private val getParsedLyricsUseCase: GetParsedLyricsUseCase,
) {
    suspend operator fun invoke(audioMedia: AudioMedia): List<LyricsLine> {
        val lines = getParsedLyricsUseCase(audioMedia.lyricFileFullPath ?: return emptyList())
        return if (audioMedia.editedAudioFileFullPath == null) lines else lines.map { it.copy(timeMs = null) }
    }
}
