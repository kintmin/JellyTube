package com.kintmin.domain.data_migration.model

/** 받는 사람이 확인 화면에서 받지 않기로 했다. 파일을 받기 전이라 남는 것이 없다. */
class MigrationDeclinedException : Exception("받기를 취소했습니다.")
