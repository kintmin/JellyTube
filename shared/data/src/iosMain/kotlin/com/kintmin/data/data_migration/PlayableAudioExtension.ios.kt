package com.kintmin.data.data_migration

/**
 * AVAudioFile 이 열고 읽는 컨테이너. webm·Vorbis·wma·amr 은 열지 못한다. Ogg Opus 는 SDK 헤더에 Ogg
 * 파일 타입이 없는 비공개 지원이라 배포 타깃(17.0)에서 보장되지 않는다. m4a 안의 Opus 는 읽기에서
 * 실패하지만 확장자로는 가를 수 없다.
 */
private val PLAYABLE_EXTENSIONS = setOf("m4a", "mp4", "mp3", "aac", "flac", "wav", "aiff", "aif", "caf")

internal actual fun isPlayableAudioExtension(extension: String): Boolean =
    extension.lowercase() in PLAYABLE_EXTENSIONS
