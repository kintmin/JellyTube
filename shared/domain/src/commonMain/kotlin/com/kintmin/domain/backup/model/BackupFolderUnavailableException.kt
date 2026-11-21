package com.kintmin.domain.backup.model

class BackupFolderUnavailableException(cause: Throwable? = null) : Exception("백업 폴더에 접근할 수 없습니다.", cause)
