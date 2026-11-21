package com.kintmin.domain.backup.model

enum class BackupFailureReason {
    /** 폴더가 지워졌거나 접근 권한이 풀렸다. 폴더를 다시 골라야 한다. */
    FOLDER_UNAVAILABLE,
    WRITE_FAILED,
}
