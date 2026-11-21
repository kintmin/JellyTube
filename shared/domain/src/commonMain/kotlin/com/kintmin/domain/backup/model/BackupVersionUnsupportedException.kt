package com.kintmin.domain.backup.model

class BackupVersionUnsupportedException(cause: Throwable? = null) : Exception("이 앱보다 새 버전에서 만든 백업입니다.", cause)
