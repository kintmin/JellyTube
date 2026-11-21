package com.kintmin.domain.backup.model

data class BackupWriteResult(
    val sizeBytes: Long,
    /** 클라우드 자동 백업 용량을 넘어 앱 안 사본은 쓰지 않았다. 고른 폴더에는 썼다. */
    val localCopySkipped: Boolean,
)
