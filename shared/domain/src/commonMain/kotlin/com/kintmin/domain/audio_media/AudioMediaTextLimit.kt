package com.kintmin.domain.audio_media

import com.kintmin.domain.extension.clampLength

/** 설명의 5000 은 유튜브 설명 상한이다. */
object AudioMediaTextLimit {
    const val MAX_NAME_LENGTH = 100
    const val MAX_ARTIST_LENGTH = 50
    const val MAX_DESCRIPTION_LENGTH = 5000

    fun clampName(name: String): String = name.clampLength(MAX_NAME_LENGTH)

    fun clampArtist(artist: String): String = artist.clampLength(MAX_ARTIST_LENGTH)

    fun clampDescription(description: String): String = description.clampLength(MAX_DESCRIPTION_LENGTH)
}
