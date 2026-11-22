package com.kintmin.desktop

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
    val status: UploadStatus = UploadStatus.QUEUED,
    /** [UploadStatus.UPLOADING] 동안의 0..1. */
    val progress: Float = 0f,
    val stageId: String? = null,
    val errorMessage: String? = null,
    val audioMediaId: Int? = null,
    val title: String? = null,
)

enum class UploadStatus {
    /** 동시 전송 수가 차서 차례를 기다린다. */
    QUEUED,
    UPLOADING,

    /** 다 보냈고, 묶음의 다른 곡이 끝나기를 기다린다. */
    STAGED,

    /** 받는 기기가 목록에 넣는 중이다. */
    SAVING,
    SUCCESS,
    FAILURE,
}
