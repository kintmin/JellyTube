package com.kintmin.desktop

import com.kintmin.desktop.update.DesktopRelease
import java.io.File
import java.util.UUID

data class MainUiState(
    val discoveryState: DiscoveryState = DiscoveryState.DISCOVERING,
    val uploadItems: List<UploadFileItem> = emptyList(),
    val bulkArtist: String = "",
    val bulkMessage: String? = null,
    val dropMessage: String? = null,
    val isApplyingBulk: Boolean = false,
    val availableUpdate: DesktopRelease? = null,
) {
    val isUploading: Boolean get() = uploadItems.any { it.status in IN_PROGRESS }
    val successCount: Int get() = uploadItems.count { it.status == UploadStatus.SUCCESS }
    val failureCount: Int get() = uploadItems.count { it.status == UploadStatus.FAILURE }
    val canApplyThumbnail: Boolean get() = !isApplyingBulk && successCount > 0
    val canApplyArtist: Boolean get() = canApplyThumbnail && bulkArtist.isNotBlank()
    val canClearList: Boolean get() = uploadItems.isNotEmpty() && !isUploading
}

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
    QUEUED,
    UPLOADING,

    /** 다 보냈고, 묶음의 다른 곡이 끝나기를 기다린다. */
    STAGED,

    /** 받는 기기가 목록에 넣는 중이다. */
    SAVING,
    SUCCESS,
    FAILURE,
}

private val IN_PROGRESS = setOf(UploadStatus.QUEUED, UploadStatus.UPLOADING, UploadStatus.STAGED, UploadStatus.SAVING)
