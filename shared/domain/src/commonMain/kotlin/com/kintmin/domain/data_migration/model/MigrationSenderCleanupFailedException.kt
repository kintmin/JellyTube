package com.kintmin.domain.data_migration.model

/** 보내는 기기가 보내기 전 데이터 정리에 실패했다. 같은 상태로 다시 시도해도 같은 곳에서 막힌다. */
class MigrationSenderCleanupFailedException(cause: Throwable? = null) :
    Exception("보내는 기기의 데이터 정리에 실패했습니다.", cause)
