package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationChoice
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationPreview
import com.kintmin.domain.data_migration.model.MigrationPreviewPlaylist
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStep
import com.kintmin.domain.data_migration.model.MigrationTrackAddition
import com.kintmin.domain.playlist.model.PlaylistType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** 보내는 기기의 데이터를 받는 기기에 합치는 계획을 세운다. [target] 에는 시스템 플레이리스트가 모두 있어야 한다. */
object MigrationPlanner {

    private val creationOrder = compareBy<MigrationAudioMedia>({ it.createdTimeMillis }, { it.id })
    private val playlistOrder = compareBy<MigrationPlaylist>({ it.sequence }, { it.id })

    // 사용자가 만든 플레이리스트는 종류와 이름이 같을 때만 합칠 수 있다. 비밀과 일반이 섞이면 안 된다.
    private fun MigrationPlaylist.mergeKey() = type to name.trim()

    /** 받는 기기가 무엇을 받게 되는지. 파일을 받기 전에 물어볼 것을 모아 준다. */
    fun preview(source: MigrationSnapshot, target: MigrationSnapshot): MigrationPreview {
        val targetAudioKeys = target.audioMedias.map { SourceKey.of(it.source) }.toSet()
        val targetUserKeys = target.playlists.filterNot { it.type.isSystem }.map { it.mergeKey() }.toSet()
        val trackCountBySourcePlaylist = source.tracks.groupingBy { it.playlistId }.eachCount()
        return MigrationPreview(
            newAudioMediaCount = source.audioMedias.map { SourceKey.of(it.source) }.toSet()
                .count { it !in targetAudioKeys },
            playlists = source.playlists.filterNot { it.type.isSystem }
                .sortedWith(playlistOrder)
                .distinctBy { it.mergeKey() }
                .map { playlist ->
                    MigrationPreviewPlaylist(
                        sourceId = playlist.id,
                        name = playlist.name,
                        audioMediaCount = trackCountBySourcePlaylist[playlist.id] ?: 0,
                        isSecret = playlist.type.isSecret,
                        isConflicting = playlist.mergeKey() in targetUserKeys,
                    )
                },
            // 플레이리스트가 하나도 없는 전송은 곡 단위 공유다.
            needsAudioMediaDestination = source.playlists.isEmpty() && source.audioMedias.isNotEmpty(),
        )
    }

    fun plan(
        source: MigrationSnapshot,
        target: MigrationSnapshot,
        choice: MigrationChoice = MigrationChoice(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        today: LocalDate = Clock.System.now().toLocalDateTime(timeZone).date,
    ): MigrationPlan {
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

        val targetUserPlaylists = target.playlists.filterNot { it.type.isSystem }
        val targetUserIdByKey = targetUserPlaylists.sortedWith(playlistOrder)
            .distinctBy { it.mergeKey() }
            .associate { it.mergeKey() to it.id }
        // 받는 사람이 고르지 않은 것은 이름이 겹쳐도 새로 만든다 — 남의 플레이리스트가 내 것에 섞이면 안 된다.
        // 미리보기는 같은 키의 플레이리스트를 하나로 보여 주므로, 고른 것은 키 단위로 따른다.
        val mergingKeys = choice.mergingPlaylistSourceIds?.let { ids ->
            source.playlists.filter { it.id in ids }.map { it.mergeKey() }.toSet()
        }
        fun MigrationPlaylist.mergeTargetId(): Int? =
            if (mergingKeys == null || mergeKey() in mergingKeys) targetUserIdByKey[mergeKey()] else null
        val sourceUserPlaylists = source.playlists.filterNot { it.type.isSystem }.sortedWith(playlistOrder)
        val newPlaylists = sourceUserPlaylists
            .filter { it.mergeTargetId() == null }
            .distinctBy { it.mergeKey() }
        val newPlaylistIdByKey = newPlaylists.associate { it.mergeKey() to it.id }
        val playlistRefBySourceId = source.playlists.associate { playlist ->
            playlist.id to if (playlist.type.isSystem) {
                MigrationRef.Existing(systemId(playlist.type))
            } else {
                playlist.mergeTargetId()?.let { MigrationRef.Existing(it) }
                    ?: MigrationRef.Incoming(newPlaylistIdByKey.getValue(playlist.mergeKey()))
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

        // 곡만 온 전송은 담을 자리가 없다. 받는 사람이 고른 플레이리스트로 보낸다.
        choice.audioMediaDestinationPlaylistId?.let { destinationId ->
            val destination = MigrationRef.Existing(destinationId)
            source.audioMedias.mapNotNull { audioRefBySourceId[it.id] }
                .filter { destination to it !in existingPairs }
                .forEach { additions.getOrPut(destination) { LinkedHashSet() } += it }
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
            // 편집 상태는 편집본 파일과 짝이다.
            editProjects = source.editProjects.filter { it.audioMediaId in editedNewAudioIds },
            steps = stepsToAdd(source, target, timeZone, today),
            requiredFiles = MigrationFiles.referencedBy(newAudioMedias, newPlaylists),
            playlistRefBySourceId = playlistRefBySourceId,
        )
    }

    /**
     * 하루 걸음수는 그날 기록의 증가분을 더한다. 기준점이 다른 기록이 한 날에 섞이면 값이 부풀어, 날짜 단위로 한쪽만 쓴다.
     * 이 기기에 기록이 있는 날과, 걸음 측정을 켜는 순간 이 기기의 센서 값이 붙는 오늘은 이 기기 쪽이다.
     */
    private fun stepsToAdd(
        source: MigrationSnapshot,
        target: MigrationSnapshot,
        timeZone: TimeZone,
        today: LocalDate,
    ): List<MigrationStep> {
        fun dateOf(step: MigrationStep) = Instant.fromEpochMilliseconds(step.createdTimeMillis).toLocalDateTime(timeZone).date
        val targetDates = target.steps.map(::dateOf).toSet()
        return source.steps.filter { step -> dateOf(step).let { it != today && it !in targetDates } }
    }
}
