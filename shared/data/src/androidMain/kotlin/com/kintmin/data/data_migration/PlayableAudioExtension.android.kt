package com.kintmin.data.data_migration

/**
 * ExoPlayer 는 확장자를 추출기 시도 순서에만 쓰고 내용을 sniff 해 고른다(media3
 * `DefaultExtractorsFactory`). WebM·Ogg 의 Opus·Vorbis 도 minSdk(26) 플랫폼 디코더로 재생한다.
 */
internal actual fun isPlayableAudioExtension(extension: String): Boolean = true
