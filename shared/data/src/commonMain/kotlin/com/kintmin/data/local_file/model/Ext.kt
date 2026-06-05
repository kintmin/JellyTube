package com.kintmin.data.local_file.model

/**
 * MP3: YouTube 다운로드 지원의 경우 실제 형식에 관계없이 .mp3 로 저장한다
 * WAV, FLAC, OGG, M4A, AAC: Quick Share 등에서 공유된 파일의 기본 확장자 보존
 * JPEG: Android 11 (SDK 30) 이하이면 사용
 * WEBP: Android 12 (SDK 31) 이상이라면 사용
 */
enum class Ext(val fileType: FileType) {
    MP3(FileType.Audio),
    WAV(FileType.Audio),
    FLAC(FileType.Audio),
    OGG(FileType.Audio),
    M4A(FileType.Audio),
    AAC(FileType.Audio),
    JPEG(FileType.Image),
    WEBP(FileType.Image),
    ;

    override fun toString() = name.lowercase()
}
