package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationScope
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.playlist.model.PlaylistType

/** 기기 전체 스냅샷에서 보낼 범위만 남긴다. 아래 계획·전송 파일 목록은 모두 이 결과에서 유도된다. */
object MigrationScopes {

    private const val SHARED_NAME_PREFIX = "공유받은 "

    fun narrow(snapshot: MigrationSnapshot, scope: MigrationScope): MigrationSnapshot {
        val playlists = when (scope) {
            is MigrationScope.Everything ->
                if (scope.includesSecret) snapshot.playlists else snapshot.playlists.filterNot { it.type.isSecret }
            is MigrationScope.Playlists ->
                snapshot.playlists.filter { it.id in scope.playlistIds }.map { it.asSharedPlaylist() }
            is MigrationScope.AudioMedias -> emptyList()
        }
        val playlistIds = playlists.map { it.id }.toSet()
        val audioMediaIds = when (scope) {
            is MigrationScope.Everything -> {
                val ids = snapshot.audioMedias.map { it.id }.toSet()
                if (scope.includesSecret) ids else ids - secretAudioMediaIds(snapshot)
            }
            is MigrationScope.Playlists ->
                snapshot.tracks.filter { it.playlistId in playlistIds }.map { it.audioMediaId }.toSet()
            is MigrationScope.AudioMedias -> scope.audioMediaIds
        }

        return MigrationSnapshot(
            audioMedias = snapshot.audioMedias.filter { it.id in audioMediaIds },
            playlists = playlists,
            tracks = snapshot.tracks.filter { it.playlistId in playlistIds && it.audioMediaId in audioMediaIds },
            editProjects = snapshot.editProjects.filter { it.audioMediaId in audioMediaIds },
            // 걸음 기록은 기기를 옮길 때만 따라간다.
            steps = if (scope is MigrationScope.Everything) snapshot.steps else emptyList(),
        )
    }

    private fun secretAudioMediaIds(snapshot: MigrationSnapshot): Set<Int> {
        val secretPlaylistIds = snapshot.playlists.filter { it.type.isSecret }.map { it.id }.toSet()
        return snapshot.tracks.filter { it.playlistId in secretPlaylistIds }.map { it.audioMediaId }.toSet()
    }

    /**
     * 받는 기기에 하나씩만 있는 시스템 플레이리스트로는 받을 수 없다 — 받는 사람의 즐겨찾기가
     * 보내는 사람 것으로 불어난다. 이름을 붙여 내 플레이리스트로 보낸다.
     */
    private fun MigrationPlaylist.asSharedPlaylist(): MigrationPlaylist = when (type) {
        PlaylistType.USER, PlaylistType.SECRET -> this
        PlaylistType.TOTAL, PlaylistType.UNCATEGORIZED, PlaylistType.FAVORITE ->
            copy(type = PlaylistType.USER, name = SHARED_NAME_PREFIX + type.defaultName)
        // 비밀 곡이 비밀모드 밖으로 나가지 않게 종류는 비밀로 남는다.
        PlaylistType.SECRET_FAVORITE ->
            copy(type = PlaylistType.SECRET, name = SHARED_NAME_PREFIX + type.defaultName)
    }
}
