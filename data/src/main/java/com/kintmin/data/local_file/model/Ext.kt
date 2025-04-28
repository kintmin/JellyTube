package com.kintmin.data.local_file.model

/**
 * MP3: 현재는 음원의 실제 형식이 뭐든 .mp3 로 저장한다.
 * JPEG: Android 11 (SDK 30) 이하라면 사용
 * WEBP: Android 12 (SDK 31) 이상이라면 사용
 */
enum class Ext(val fileType: FileType) {
    MP3(FileType.Audio),
    JPEG(FileType.Image),
    WEBP(FileType.Image),
    ;

    override fun toString() = name.lowercase()
}