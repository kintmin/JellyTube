package com.kintmin.domain.usecase

import javax.inject.Inject

class ExtractYoutubeVideoIdUseCase @Inject constructor() {
    operator fun invoke(youtubeUrl: String): String {
        val regex = Regex("v=([a-zA-Z0-9_-]{11})")
        return regex.find(youtubeUrl)?.groupValues?.get(1) ?: throw Exception("유튜브 url 형식이 아닙니다.")
    }
}