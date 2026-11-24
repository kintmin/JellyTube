package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationScope
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.playlist.model.PlaylistType

/**
 * 스냅샷에서 주고받을 범위만 남긴다. 계획과 전송 파일 목록은 모두 이 결과에서 유도된다.
 *
 * 받는 쪽이 비밀을 받지 않기로 했을 때도 같은 연산이다 — [MigrationScope.Everything] 의
 * `includesSecret = false` 로 부른다.
 */
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
                if (scope.includesSecret) ids else ids - snapshot.secretAudioMediaIds()
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

    /** 이 플레이리스트가 받는 기기에서 가질 이름. 고르는 화면이 미리 보여 주는 이름도 이것이다. */
    fun sharedNameOf(type: PlaylistType, name: String): String =
        if (type == PlaylistType.USER || type == PlaylistType.SECRET) name else SHARED_NAME_PREFIX + type.defaultName

    /**
     * 받는 기기에 하나씩만 있는 시스템 플레이리스트로는 받을 수 없다 — 받는 사람의 즐겨찾기가
     * 보내는 사람 것으로 불어난다. 이름을 붙여 내 플레이리스트로 보낸다.
     */
    private fun MigrationPlaylist.asSharedPlaylist(): MigrationPlaylist = when (type) {
        PlaylistType.USER, PlaylistType.SECRET -> this
        PlaylistType.TOTAL, PlaylistType.UNCATEGORIZED, PlaylistType.FAVORITE ->
            copy(type = PlaylistType.USER, name = sharedNameOf(type, name))
        // 비밀 곡이 비밀모드 밖으로 나가지 않게 종류는 비밀로 남는다.
        PlaylistType.SECRET_FAVORITE ->
            copy(type = PlaylistType.SECRET, name = sharedNameOf(type, name))
    }
}

internal fun MigrationSnapshot.secretAudioMediaIds(): Set<Int> {
    val secretPlaylistIds = playlists.filter { it.type.isSecret }.map { it.id }.toSet()
    return tracks.filter { it.playlistId in secretPlaylistIds }.map { it.audioMediaId }.toSet()
}
