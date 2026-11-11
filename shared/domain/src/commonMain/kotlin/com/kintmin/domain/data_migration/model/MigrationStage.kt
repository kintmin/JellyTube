package com.kintmin.domain.data_migration.model

/** 받는 기기에서 이전이 지나는 단계. 이 순서대로 진행한다. */
enum class MigrationStage {
    /** 보내는 기기의 목록을 받아 무엇을 들여올지 정한다. */
    PLANNING,

    /** 이 기기에서 재생할 수 없는 형식을 원본에서 다시 받는다. */
    REDOWNLOAD,

    /** 파일을 임시 공간에 받는다. */
    TRANSFER,

    /** 받은 곡만 한 트랜잭션으로 DB 에 넣고 다운로드 설정을 반영한다. */
    APPLY,
}
