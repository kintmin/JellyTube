package com.kintmin.domain.audio_media.format

/**
 * 파일 내용으로 판별한 음원 컨테이너와 그에 맞는 확장자.
 *
 * iOS 의 AVAudioFile 은 확장자를 형식 힌트로 쓴다. 내용과 다르면(m4a 를 `.mp3` 로) 열지 못하고,
 * flac 을 `.mp3` 로 두면 mp1 로 잘못 열린 뒤 읽기에서 실패한다. 그래서 확장자는 내용에서 정한다.
 */
enum class AudioContainer(val extension: String) {
    MP4("m4a"),
    MP3("mp3"),
    ADTS("aac"),
    FLAC("flac"),
    WAVE("wav"),
    AIFF("aiff"),
    CAF("caf"),
    OGG_OPUS("opus"),
    OGG("ogg"),
    WEBM("webm"),
    ASF("wma"),
    AMR("amr"),
}
