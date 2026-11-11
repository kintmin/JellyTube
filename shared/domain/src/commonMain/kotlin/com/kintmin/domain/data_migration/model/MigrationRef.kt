package com.kintmin.domain.data_migration.model

/** 계획이 가리키는 행. 새로 들어갈 행은 반영 전까지 id 가 없어 보내는 쪽 id 로 가리킨다. */
sealed interface MigrationRef {

    /** 받는 기기에 이미 있는 행의 id. */
    data class Existing(val id: Int) : MigrationRef

    /** 새로 삽입할 행. 보내는 기기의 id. */
    data class Incoming(val sourceId: Int) : MigrationRef
}
