package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.EditedLyricsRow
import com.kintmin.domain.lyrics.model.LyricsLine

/** 편집한 가사를 싱크 가사로 저장한다. 번역·음차는 있을 때만 원본과 같은 타임코드로 따로 저장한다. */
class SaveEditedLyricsUseCase(
    private val serializeLyricsUseCase: SerializeLyricsUseCase,
    private val applyLyricsToAudioMediaUseCase: ApplyLyricsToAudioMediaUseCase,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        rows: List<EditedLyricsRow>,
        hasTranslation: Boolean,
        hasTransliteration: Boolean,
        previousLyricFileFullPath: String?,
    ): Result<String> {
        fun serialize(text: (EditedLyricsRow) -> String) =
            serializeLyricsUseCase(rows.map { LyricsLine(timeMs = it.timeMs, text = text(it)) })

        return applyLyricsToAudioMediaUseCase(
            audioMediaId = audioMediaId,
            plainLyrics = null,
            syncedLyrics = serialize { it.text },
            translationLyrics = if (hasTranslation) serialize { it.translation } else null,
            transliterationLyrics = if (hasTransliteration) serialize { it.transliteration } else null,
            previousLyricFileFullPath = previousLyricFileFullPath,
        )
    }
}
