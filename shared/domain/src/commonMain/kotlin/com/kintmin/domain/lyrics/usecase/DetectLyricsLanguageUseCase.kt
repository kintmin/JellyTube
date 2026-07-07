package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsLanguage
import com.kintmin.domain.lyrics.model.LyricsLine

/**
 * 가사 줄 목록에서 한국어/일본어 포함 여부를 판별한다.
 * - 한글: 음절(가-힣), 한글 자모, 한글 호환 자모
 * - 일본어: 히라가나/가타카나 (한자는 중국어와 공유되므로 신호로 쓰지 않는다)
 * - sourceLanguage: 일본어가 있으면 "ja", 그 외에는 "en" (휴리스틱)
 */
class DetectLyricsLanguageUseCase {

    operator fun invoke(lines: List<LyricsLine>): LyricsLanguage {
        var hasKorean = false
        var hasJapanese = false

        for (line in lines) {
            for (char in line.text) {
                when (char.code) {
                    in 0xAC00..0xD7A3, // 한글 음절
                    in 0x1100..0x11FF, // 한글 자모
                    in 0x3130..0x318F, // 한글 호환 자모
                    -> hasKorean = true

                    in 0x3040..0x309F, // 히라가나
                    in 0x30A0..0x30FF, // 가타카나
                    -> hasJapanese = true
                }
            }
        }

        return LyricsLanguage(
            hasKorean = hasKorean,
            hasJapanese = hasJapanese,
            sourceLanguage = if (hasJapanese) "ja" else "en",
        )
    }
}
