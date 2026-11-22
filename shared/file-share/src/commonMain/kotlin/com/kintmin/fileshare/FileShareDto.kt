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
data class UploadStageResponse(
    val success: Boolean,
    val message: String,
    val stageId: String? = null,
)

@Serializable
data class UploadCommitRequest(
    val stageIds: List<String>,
)

/** [UploadCommitRequest.stageIds] 와 같은 순서다. */
@Serializable
data class UploadCommitResponse(
    val results: List<UploadResponse>,
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
