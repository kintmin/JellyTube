package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.repository.LyricsTranslationRepository

/**
 * 원본 가사로부터 번역/음차 변형 파일을 생성해 저장한다.
 * 원본을 파싱해 텍스트만 엔진에 넘기고, 결과를 원본 타임스탬프와 정렬해 다시 직렬화한다.
 * 원본이 싱크(LRC)면 타임스탬프를 유지해 LRC 로, 비싱크면 일반 텍스트로 저장한다.
 */
class CreateLyricsVariantUseCase(
    private val audioMediaRepository: AudioMediaRepository,
    private val lyricsTranslationRepository: LyricsTranslationRepository,
    private val parseLyricsUseCase: ParseLyricsUseCase,
    private val serializeLyricsUseCase: SerializeLyricsUseCase,
) {
    suspend operator fun invoke(
        lyricFileFullPath: String,
        variant: LyricsVariant,
        sourceLanguage: String,
    ): Result<Unit> = runCatching {
        val rawLyrics = audioMediaRepository.getLyrics(lyricFileFullPath).getOrThrow()
        val parsedLines = parseLyricsUseCase(rawLyrics)
        if (parsedLines.all { it.text.isBlank() }) error("변환할 가사가 없습니다.")

        val sourceText = parsedLines.joinToString("\n") { it.text }
        val convertedText = when (variant) {
            LyricsVariant.TRANSLATION ->
                lyricsTranslationRepository.translate(sourceText, sourceLanguage).getOrThrow()

            LyricsVariant.TRANSLITERATION ->
                lyricsTranslationRepository.transliterate(sourceText).getOrThrow()
        }

        // 엔진 결과를 원본 줄 순서에 맞춰 정렬한다. (줄 수가 어긋나면 원본 텍스트로 대체)
        val convertedLines = convertedText.split("\n")
        val variantLines = parsedLines.mapIndexed { index, line ->
            line.copy(text = convertedLines.getOrElse(index) { line.text })
        }

        val synced = parsedLines.any { it.timeMs != null }
        val outputText = if (synced) {
            serializeLyricsUseCase(variantLines)
        } else {
            variantLines.joinToString("\n") { it.text }
        }

        audioMediaRepository.saveVariantLyrics(lyricFileFullPath, variant, outputText).getOrThrow()
    }
}
