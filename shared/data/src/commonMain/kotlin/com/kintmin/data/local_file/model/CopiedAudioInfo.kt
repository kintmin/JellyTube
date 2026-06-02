package com.kintmin.data.local_file.model

data class CopiedAudioInfo(
    val fileNameWithExt: String,
    val sha256Hex: String,
    val title: String?,
    val artist: String?,
    val durationMs: Long?,
    val imageFileNameWithExt: String? = null,
)
