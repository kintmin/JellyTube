package com.kintmin.domain.backup.model

data class BackupStatus(
    val folder: BackupFolder?,
    val lastSuccessMillis: Long?,
    val lastFailureMillis: Long?,
    val lastFailureReason: BackupFailureReason?,
) {
    /** 저장소가 값을 지우지 못해 실패 기록은 남아 있다. 그 뒤에 성공했으면 실패가 아니다. */
    val isFailing: Boolean
        get() = lastFailureMillis != null && lastFailureMillis > (lastSuccessMillis ?: Long.MIN_VALUE)
}
