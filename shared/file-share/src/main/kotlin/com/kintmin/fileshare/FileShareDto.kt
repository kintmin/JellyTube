package com.kintmin.fileshare

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val success: Boolean,
    val message: String,
    val audioMediaId: Int? = null,
    val title: String? = null,
)

@Serializable
data class BulkArtistUpdateRequest(
    val audioMediaIds: List<Int>,
    val artist: String,
)

@Serializable
data class FileShareResponse(
    val success: Boolean,
    val message: String,
)

enum class UploadStatus {
    IDLE,
    UPLOADING,
    SUCCESS,
    FAILURE,
}
