package com.kintmin.domain.data_migration.model

enum class MigrationFailureReason {
    /** 이 기기에서 재생할 수 없는 형식인데 원본 주소가 유튜브가 아니라 다시 받을 수 없다. */
    UNSUPPORTED_FORMAT,

    /** 재생할 수 없는 형식이라 원본에서 다시 받으려 했으나 실패했다. */
    REDOWNLOAD_FAILED,

    /** 보내는 기기에서 음원 파일을 받지 못했다(파일이 없거나 크기가 다르다). */
    TRANSFER_FAILED,
}
