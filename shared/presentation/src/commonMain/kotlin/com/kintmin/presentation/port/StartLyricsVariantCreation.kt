package com.kintmin.presentation.port

import com.kintmin.domain.lyrics.model.LyricsVariant

/** 가사 번역·음차 변형 생성을 백그라운드 작업으로 넘긴다. */
interface StartLyricsVariantCreation {

    operator fun invoke(
        audioMediaId: Int,
        lyricFileFullPath: String,
        variant: LyricsVariant,
    )
}
