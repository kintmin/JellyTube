package com.kintmin.platform.util.model

data class MediaControlData(
    val mediaId: String,
    val mediaFileUri: String,
    val mediaTitle: String,
    val mediaDescription: String,
    val mediaArtist: String,
    val mediaDurationMs: Long,
    val mediaArtworkFileUri: String?,
)