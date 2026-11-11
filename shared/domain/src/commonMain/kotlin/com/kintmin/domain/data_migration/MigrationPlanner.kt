package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationTrackAddition
import com.kintmin.domain.playlist.model.PlaylistType

/** 보내는 기기의 데이터를 받는 기기에 합치는 계획을 세운다. [target] 에는 시스템 플레이리스트가 모두 있어야 한다. */
object MigrationPlanner {

    private val creationOrder = compareBy<MigrationAudioMedia>({ it.createdTimeMillis }, { it.id })
    private val playlistOrder = compareBy<MigrationPlaylist>({ it.sequence }, { it.id })

    fun plan(source: MigrationSnapshot, target: MigrationSnapshot): MigrationPlan {
        val targetAudioIdByKey = target.audioMedias.sortedWith(creationOrder)
            .distinctBy { SourceKey.of(it.source) }
            .associate { SourceKey.of(it.source) to it.id }
        val canonicalSourceAudios = source.audioMedias.sortedWith(creationOrder)
            .distinctBy { SourceKey.of(it.source) }
        val canonicalSourceIdByKey = canonicalSourceAudios.associate { SourceKey.of(it.source) to it.id }
        val newAudioMedias = canonicalSourceAudios.filter { SourceKey.of(it.source) !in targetAudioIdByKey }
        val audioRefBySourceId = source.audioMedias.associate { audio ->
            val key = SourceKey.of(audio.source)
            audio.id to (targetAudioIdByKey[key]?.let { MigrationRef.Existing(it) }
                ?: MigrationRef.Incoming(canonicalSourceIdByKey.getValue(key)))
        }

        val targetSystemIdByType = target.playlists.filter { it.type.isSystem }
            .sortedBy { it.id }
            .distinctBy { it.type }
            .associate { it.type to it.id }
        fun systemId(type: PlaylistType) =
            targetSystemIdByType[type] ?: error("받는 기기에 ${type.name} 플레이리스트가 없습니다.")

        val targetUserPlaylists = target.playlists.filter { it.type == PlaylistType.USER }
        val targetUserIdByName = targetUserPlaylists.sortedWith(playlistOrder)
            .distinctBy { it.name.trim() }
            .associate { it.name.trim() to it.id }
        val sourceUserPlaylists = source.playlists.filter { it.type == PlaylistType.USER }.sortedWith(playlistOrder)
        val newPlaylists = sourceUserPlaylists
            .filter { it.name.trim() !in targetUserIdByName }
            .distinctBy { it.name.trim() }
        val newPlaylistIdByName = newPlaylists.associate { it.name.trim() to it.id }
        val playlistRefBySourceId = source.playlists.associate { playlist ->
            playlist.id to when (playlist.type) {
                PlaylistType.USER -> targetUserIdByName[playlist.name.trim()]?.let { MigrationRef.Existing(it) }
                    ?: MigrationRef.Incoming(newPlaylistIdByName.getValue(playlist.name.trim()))
                else -> MigrationRef.Existing(systemId(playlist.type))
            }
        }

        val existingPairs = target.tracks
            .map { MigrationRef.Existing(it.playlistId) to MigrationRef.Existing(it.audioMediaId) }
            .toSet()
        val sourcePlaylistOrder = source.playlists.sortedWith(playlistOrder).withIndex()
            .associate { (index, playlist) -> playlist.id to index }
        val additions = LinkedHashMap<MigrationRef, LinkedHashSet<MigrationRef>>()
        source.tracks
            .filter { track -> source.playlists.none { it.id == track.playlistId && it.type == PlaylistType.UNCATEGORIZED } }
            .sortedWith(compareBy({ sourcePlaylistOrder[it.playlistId] ?: Int.MAX_VALUE }, { it.sequence }))
            .forEach { track ->
                val playlist = playlistRefBySourceId[track.playlistId] ?: return@forEach
                val audio = audioRefBySourceId[track.audioMediaId] ?: return@forEach
                if (playlist to audio !in existingPairs) {
                    additions.getOrPut(playlist) { LinkedHashSet() } += audio
                }
            }

        // 보내는 쪽 전체 목록이 어긋나 있어도 새 곡은 반드시 전체에 들어가야 한다.
        additions.getOrPut(MigrationRef.Existing(systemId(PlaylistType.TOTAL))) { LinkedHashSet() } +=
            newAudioMedias.map { MigrationRef.Incoming(it.id) }

        val targetUserIds = targetUserPlaylists.map { it.id }.toSet()
        fun isUser(ref: MigrationRef) = ref is MigrationRef.Incoming ||
            (ref is MigrationRef.Existing && ref.id in targetUserIds)
        val categorized: Set<MigrationRef> =
            target.tracks.filter { it.playlistId in targetUserIds }.map { MigrationRef.Existing(it.audioMediaId) }.toSet() +
                additions.filterKeys(::isUser).values.flatten()

        val uncategorizedId = systemId(PlaylistType.UNCATEGORIZED)
        val newUncategorized = newAudioMedias.map { MigrationRef.Incoming(it.id) }.filter { it !in categorized }
        if (newUncategorized.isNotEmpty()) {
            additions.getOrPut(MigrationRef.Existing(uncategorizedId)) { LinkedHashSet() } += newUncategorized
        }
        val uncategorizedRemovals = target.tracks
            .filter { it.playlistId == uncategorizedId && MigrationRef.Existing(it.audioMediaId) in categorized }
            .map { it.audioMediaId }

        val editedNewAudioIds = newAudioMedias.filter { it.editedAudioFileName != null }.map { it.id }.toSet()
        return MigrationPlan(
            newAudioMedias = newAudioMedias,
            newPlaylists = newPlaylists,
            trackAdditions = additions.filterValues { it.isNotEmpty() }
                .map { (playlist, audios) -> MigrationTrackAddition(playlist, audios.toList()) },
            uncategorizedRemovals = uncategorizedRemovals,
            // 편집 상태는 편집본 파일과 짝이다. 편집본 없이 들어오면 있지도 않은 편집본을 기준으로 복원한다.
            editProjects = source.editProjects.filter { it.audioMediaId in editedNewAudioIds },
            steps = source.steps,
            requiredFiles = MigrationFiles.referencedBy(newAudioMedias, newPlaylists),
            playlistRefBySourceId = playlistRefBySourceId,
        )
    }
}
