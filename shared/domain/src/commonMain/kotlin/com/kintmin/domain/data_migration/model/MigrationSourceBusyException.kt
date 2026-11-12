package com.kintmin.domain.data_migration.model

/** 보내는 기기가 이미 다른 기기와 페어링해 보내고 있다. */
class MigrationSourceBusyException : Exception("보내는 기기가 이미 다른 기기와 연결돼 있습니다.")
