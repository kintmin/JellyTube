package com.kintmin.presentation.port.model

import com.kintmin.domain.lyrics.model.LyricsVariant

/** 작업 알림에 쓰는 이름. */
val LyricsVariant.label: String
    get() = when (this) {
        LyricsVariant.TRANSLATION -> "가사 번역"
        LyricsVariant.TRANSLITERATION -> "가사 음차 번역"
    }
