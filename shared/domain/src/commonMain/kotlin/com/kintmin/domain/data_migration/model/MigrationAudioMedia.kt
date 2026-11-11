package com.kintmin.domain.data_migration.model

/** 이전 대상 음원 한 행. 파일은 경로가 아니라 이름이다 — 두 기기의 저장 위치가 다르다. */
data class MigrationAudioMedia(
    val id: Int,
    val source: String,
    val name: String,
    val artist: String,
    val description: String,
    val rawAudioDurationSeconds: Long?,
    val audioFileName: String,
    val imageFileName: String?,
    val lyricFileName: String?,
    val tjKaraokeNumber: String?,
    val editedAudioFileName: String?,
    val editedAudioDurationSeconds: Long?,
    val createdTimeMillis: Long,
)
