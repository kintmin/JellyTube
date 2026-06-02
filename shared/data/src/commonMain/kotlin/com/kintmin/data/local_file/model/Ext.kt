package com.kintmin.data.local_file.model

/**
 * MP3: YouTube ?¤ìš´ë¡œë“œ ?Œì›?€ ?¤ì œ ?•ì‹??ê´€ê³„ì—†??.mp3 ë¡??€?¥í•œ??
 * WAV, FLAC, OGG, M4A, AAC: Quick Share ???¸ë? ê³µìœ ë¡?ê°€?¸ì˜¨ ?Œì¼???ë³¸ ?•ì¥??ë³´ì¡´??
 * JPEG: Android 11 (SDK 30) ?´í•˜?¼ë©´ ?¬ìš©
 * WEBP: Android 12 (SDK 31) ?´ìƒ?´ë¼ë©??¬ìš©
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