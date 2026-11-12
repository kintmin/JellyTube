package com.kintmin.domain.data_migration.model

/** 페어링하려는데 보내는 기기에 열린 이전 세션이 없다. 보내기 화면이 닫혔거나 코드를 여러 번 틀려 잠겼다. */
class MigrationSourceClosedException : Exception("보내는 기기의 보내기 화면이 닫혔습니다.")
