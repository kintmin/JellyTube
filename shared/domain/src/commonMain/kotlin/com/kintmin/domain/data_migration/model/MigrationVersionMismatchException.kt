package com.kintmin.domain.data_migration.model

class MigrationVersionMismatchException : Exception("두 기기의 앱 버전이 달라 옮길 수 없습니다.")
