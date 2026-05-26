package com.kintmin.fileshare

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val success: Boolean,
    val message: String,
    val audioMediaId: Int? = null,
    val title: String? = null,
)

enum class UploadStatus {
    IDLE,
    UPLOADING,
    SUCCESS,
    FAILURE,
}
