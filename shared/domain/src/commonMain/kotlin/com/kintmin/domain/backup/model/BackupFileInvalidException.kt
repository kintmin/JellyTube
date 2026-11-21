package com.kintmin.domain.backup.model

class BackupFileInvalidException(cause: Throwable? = null) : Exception("백업 파일을 읽을 수 없습니다.", cause)
