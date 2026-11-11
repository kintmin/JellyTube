package com.kintmin.data.data_migration

/** 이 플랫폼의 플레이어가 여는 음원 확장자인지. 확장자가 내용과 맞다는 전제다. */
internal expect fun isPlayableAudioExtension(extension: String): Boolean
