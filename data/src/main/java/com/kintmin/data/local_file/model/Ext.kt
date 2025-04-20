package com.kintmin.data.local_file.model

enum class Ext(val fileType: FileType) {
    MP3(FileType.Audio),
    M4A(FileType.Audio),
    WEBM(FileType.Audio),
    OPUS(FileType.Audio),
    JPG(FileType.Image),
    JPEG(FileType.Image),
    PNG(FileType.Image),
    WEBP(FileType.Image),
    ;

    override fun toString() = name.lowercase()
}