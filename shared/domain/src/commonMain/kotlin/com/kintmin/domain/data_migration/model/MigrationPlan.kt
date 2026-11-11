package com.kintmin.domain.data_migration.model

data class MigrationPlan(
    val newAudioMedias: List<MigrationAudioMedia>,
    /** 보내는 쪽 순서대로. 받는 쪽 목록 맨 뒤에 붙인다. */
    val newPlaylists: List<MigrationPlaylist>,
    val trackAdditions: List<MigrationTrackAddition>,
    /** 이전으로 USER 플레이리스트에 들어가게 된 기존 곡. 미분류에서 뺀다. */
    val uncategorizedRemovals: List<Int>,
    val editProjects: List<MigrationEditProject>,
    val steps: List<MigrationStep>,
    val requiredFiles: Set<MigrationFile>,
    /** 보내는 쪽 플레이리스트 id 가 받는 쪽 어디로 가는지. 다운로드 대상 설정을 옮길 때 쓴다. */
    val playlistRefBySourceId: Map<Int, MigrationRef>,
)
