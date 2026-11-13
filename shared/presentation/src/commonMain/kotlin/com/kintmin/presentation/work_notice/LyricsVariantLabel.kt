package com.kintmin.presentation.work_notice

import com.kintmin.domain.lyrics.model.LyricsVariant

/** 작업 알림에 쓰는 이름. 두 플랫폼이 같은 말을 써야 해서 여기 한 벌만 둔다. */
val LyricsVariant.label: String
    get() = when (this) {
        LyricsVariant.TRANSLATION -> "가사 번역"
        LyricsVariant.TRANSLITERATION -> "가사 음차 번역"
    }
