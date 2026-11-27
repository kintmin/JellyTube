package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.lyrics.model.EditedAudioLyrics
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.model.RemappedLyricsLine

/**
 * 원본 가사를 편집 배치에 맞춰 옮겨 LRC 문자열로 만든다. 옮길 가사가 없으면 null.
 *
 * 원본 자리에 덮어쓸 때와 새 곡으로 저장할 때가 이것을 함께 쓴다 — 두 경로가 같은 조각 목록을
 * 받으므로 들은 소리와 가사가 어긋나지 않는다.
 */
class BuildEditedAudioLyricsUseCase(
    private val getParsedLyricsUseCase: GetParsedLyricsUseCase,
    private val getLyricsVariantUseCase: GetLyricsVariantUseCase,
    private val remapLyricsToAudioEditUseCase: RemapLyricsToAudioEditUseCase,
    private val serializeLyricsUseCase: SerializeLyricsUseCase,
) {
    suspend operator fun invoke(
        lyricFileFullPath: String,
        parts: List<AudioEditPart>,
    ): EditedAudioLyrics? {
        val lines = getParsedLyricsUseCase(lyricFileFullPath)
        if (lines.isEmpty()) return null

        val remapped = if (lines.any { (it.timeMs ?: 0L) > 0L }) {
            remapLyricsToAudioEditUseCase(lines, parts)
        } else {
            // 흐르지 않는 가사는 옮길 시간축이 없다. 편집본에도 글로만 남는다.
            lines.indices.map { RemappedLyricsLine(sourceIndex = it, timeMs = 0L) }
        }
        if (remapped.isEmpty()) return null

        fun serialize(source: List<LyricsLine>): String = serializeLyricsUseCase(
            remapped.map { LyricsLine(timeMs = it.timeMs, text = source.getOrNull(it.sourceIndex)?.text.orEmpty()) }
        )

        val translation = getLyricsVariantUseCase(lyricFileFullPath, LyricsVariant.TRANSLATION).getOrNull()
        val transliteration = getLyricsVariantUseCase(lyricFileFullPath, LyricsVariant.TRANSLITERATION).getOrNull()

        return EditedAudioLyrics(
            synced = serialize(lines),
            translation = translation?.let { serialize(it) },
            transliteration = transliteration?.let { serialize(it) },
        )
    }
}
