package com.kintmin.domain.data_migration.model

/** 코드가 틀렸거나 이미 다른 기기와 페어링됐다. */
class MigrationPairRejectedException : Exception("보내는 기기가 연결을 거부했습니다.")
