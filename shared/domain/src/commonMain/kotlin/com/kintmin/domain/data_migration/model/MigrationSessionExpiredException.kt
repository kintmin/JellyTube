package com.kintmin.domain.data_migration.model

/** 보내는 기기의 세션이 바뀌었거나 닫혔다. 새 코드로 다시 연결해야 한다. */
class MigrationSessionExpiredException(cause: Throwable? = null) :
    Exception("보내는 기기와의 연결이 끊겼습니다.", cause)
