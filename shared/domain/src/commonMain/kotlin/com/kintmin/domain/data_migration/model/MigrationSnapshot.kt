package com.kintmin.domain.data_migration.model

/** 한 기기의 DB 를 이전 비교에 필요한 만큼 옮긴 것. */
data class MigrationSnapshot(
    val audioMedias: List<MigrationAudioMedia>,
    val playlists: List<MigrationPlaylist>,
    val tracks: List<MigrationTrack>,
    val editProjects: List<MigrationEditProject>,
    val steps: List<MigrationStep>,
)
