package com.kintmin.data.python_bridge

import com.kintmin.domain.audio_media.model.AgeRestrictedMedia

/** 브릿지는 yt-dlp 오류를 문구로만 넘긴다. 다시 시도해도 소용없는 오류만 골라 도메인 예외로 바꾼다. */
internal object YtDlpError {

    private const val AGE_RESTRICTED = "confirm your age"

    fun classify(error: Throwable): Throwable =
        if (error.message?.contains(AGE_RESTRICTED) == true) AgeRestrictedMedia() else error
}
