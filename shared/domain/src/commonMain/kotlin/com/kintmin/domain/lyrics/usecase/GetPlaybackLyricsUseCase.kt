package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.lyrics.model.PlaybackLyrics

/**
 * 재생 위치에 맞춰 보여 줄 가사를 읽는다. 가사가 없으면 null.
 *
 * 편집본인데 편집본 가사가 아직 없으면(옛 데이터나 복원 직후) 원본 가사를 **흐르지 않게** 돌려준다.
 * 원본 시간축이라 소리와 어긋나는데, 틀린 자리를 짚는 것보다 글로만 보여 주는 쪽이 낫다.
 */
class GetPlaybackLyricsUseCase(
    private val getParsedLyricsUseCase: GetParsedLyricsUseCase,
) {
    suspend operator fun invoke(audioMedia: AudioMedia): PlaybackLyrics? {
        val path = audioMedia.playbackLyricFileFullPath ?: return null
        val lines = getParsedLyricsUseCase(path)
        val keepsSync = audioMedia.editedAudioFileFullPath == null || audioMedia.editedLyricFileFullPath != null
        return PlaybackLyrics(
            lyricFileFullPath = path,
            lines = if (keepsSync) lines else lines.map { it.copy(timeMs = null) },
        )
    }
}
