package com.kintmin.desktop

import com.kintmin.fileshare.UploadStatus
import java.io.File
import java.util.UUID

data class MainUiState(
    val discoveryState: DiscoveryState = DiscoveryState.DISCOVERING,
    val uploadItems: List<UploadFileItem> = emptyList(),
    val bulkArtist: String = "",
    val bulkMessage: String? = null,
)

enum class DiscoveryState { DISCOVERING, FOUND, NOT_FOUND }

data class UploadFileItem(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val status: UploadStatus = UploadStatus.UPLOADING,
    val errorMessage: String? = null,
    val audioMediaId: Int? = null,
    val title: String? = null,
)