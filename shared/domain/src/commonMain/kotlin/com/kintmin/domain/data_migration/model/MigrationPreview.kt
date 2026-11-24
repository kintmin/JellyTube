package com.kintmin.domain.data_migration.model

/** 받는 기기가 파일을 받기 전에 보는 요약. 이것을 보고 [MigrationChoice] 를 정한다. */
data class MigrationPreview(
    val newAudioMediaCount: Int,
    val playlists: List<MigrationPreviewPlaylist>,
    /** 플레이리스트 없이 곡만 왔다. 담을 자리를 받는 사람이 정해야 한다. */
    val needsAudioMediaDestination: Boolean,
) {
    val conflictingPlaylists: List<MigrationPreviewPlaylist> get() = playlists.filter { it.isConflicting }
    val secretPlaylistCount: Int get() = playlists.count { it.isSecret }
}

data class MigrationPreviewPlaylist(
    /** 보내는 기기의 id. [MigrationChoice.mergingPlaylistSourceIds] 가 이것을 가리킨다. */
    val sourceId: Int,
    val name: String,
    val audioMediaCount: Int,
    val isSecret: Boolean,
    /** 받는 기기에 같은 이름·종류가 이미 있다. */
    val isConflicting: Boolean,
)
