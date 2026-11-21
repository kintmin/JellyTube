package com.kintmin.domain.backup.model

class BackupFolderNotRegisteredException(cause: Throwable? = null) : Exception("백업 폴더가 지정되지 않았습니다.", cause)
