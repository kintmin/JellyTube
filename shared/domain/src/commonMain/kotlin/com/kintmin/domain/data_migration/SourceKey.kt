package com.kintmin.domain.data_migration

import com.kintmin.domain.youtube.YoutubeUrl

/**
 * 두 기기의 음원이 같은 곡인지 가르는 키. 저장된 source 는 공유받은 주소 그대로라
 * 같은 영상도 기기마다 문자열이 다르다. 저장값은 바꾸지 않고 비교할 때만 쓴다.
 */
object SourceKey {

    fun of(source: String): String =
        YoutubeUrl.videoIdOf(source)?.let { "youtube:$it" } ?: source
}
