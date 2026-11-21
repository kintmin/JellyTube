package com.kintmin.presentation.ui.setting.backup_restore

import com.kintmin.domain.backup.model.BackupFailureReason
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.extension.toLocalDateTime
import com.kintmin.presentation.extension.toKoreanDateTimeStringWithoutSecond

fun BackupStatus.toDescription(): String {
    val lastSuccess = lastSuccessMillis
    return when {
        folder == null -> "백업 폴더를 지정하세요"
        isFailing && lastFailureReason == BackupFailureReason.FOLDER_UNAVAILABLE -> "백업 폴더에 접근할 수 없습니다. 폴더를 다시 지정하세요"
        isFailing -> "마지막 백업에 실패했습니다"
        lastSuccess != null -> "마지막 백업 ${lastSuccess.toDateTimeText()}"
        else -> "아직 백업하지 않았습니다"
    }
}

fun Long.toDateTimeText(): String = toLocalDateTime().toKoreanDateTimeStringWithoutSecond()
