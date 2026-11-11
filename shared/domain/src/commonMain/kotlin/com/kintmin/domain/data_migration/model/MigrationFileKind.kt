package com.kintmin.domain.data_migration.model

/** 파일 종류. 디렉터리 이름이 아니다 — 저장 위치는 기기마다 다르다. */
enum class MigrationFileKind {
    AUDIO,
    IMAGE,
    LYRIC,
}
