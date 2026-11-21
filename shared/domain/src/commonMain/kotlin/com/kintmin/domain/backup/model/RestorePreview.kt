package com.kintmin.domain.backup.model

data class RestorePreview(
    val createdAtMillis: Long,
    val totalCount: Int,
    val alreadyOnDeviceCount: Int,
    val redownloadCount: Int,
    /** 원본이 유튜브 주소가 아니라 다시 받을 수 없는 곡. */
    val notRestorableCount: Int,
    val estimatedDownloadBytes: Long,
)
