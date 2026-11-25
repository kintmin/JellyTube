package com.kintmin.data.local_db.dao_facade

import androidx.room.deferredTransaction
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import com.kintmin.data.local_db.dao.AudioEditProjectDao
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.dao.StepDao
import com.kintmin.data.local_db.mapper.toMigration
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioEditProjectEntity
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.displayImageFileNameWithExt
import com.kintmin.data.local_db.model.clampText
import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.data.local_db.model.StepEntity
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.playlist.model.PlaylistType

class AudioMediaFacade constructor(
    private val db: JellyTubeDatabase,
    private val audioMediaDao: AudioMediaDao,
    private val playlistDao: PlaylistDao,
    private val playlistTrackDao: PlaylistTrackDao,
    private val audioEditProjectDao: AudioEditProjectDao,
    private val stepDao: StepDao,
) {

    /** 다섯 테이블을 한 읽기 트랜잭션에서 읽는다. 따로 읽으면 사이에 끼어든 쓰기로 소속이 없는 곡을 가리킬 수 있다. */
    suspend fun fetchMigrationSnapshot(): MigrationSnapshot {
        return db.useReaderConnection { transactor ->
            transactor.deferredTransaction {
                MigrationSnapshot(
                    audioMedias = audioMediaDao.getAll().map { it.toMigration() },
                    playlists = playlistDao.getAll().map { it.toMigration() },
                    tracks = playlistTrackDao.getAll().map { it.toMigration() },
                    editProjects = audioEditProjectDao.getAll().map { it.toMigration() },
                    steps = stepDao.getAll().map { it.toMigration() },
                )
            }
        }
    }

    /**
     * 새 행은 여기서 새 id 를 받는다. 소속은 기존 추가 경로로 붙여 곡 수·재생시간·대표 이미지를 맞춘다.
     * 새로 만든 플레이리스트의 보내는 쪽 id → 새 id 를 돌려준다.
     */
    suspend fun applyMigrationPlan(plan: MigrationPlan): Map<Int, Int> {
        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val audioIdBySourceId = plan.newAudioMedias.associate { audio ->
                    audio.id to audioMediaDao.insertAudioMedia(
                        AudioMediaEntity(
                            source = audio.source,
                            name = audio.name,
                            artist = audio.artist,
                            description = audio.description,
                            rawAudioDurationSeconds = audio.rawAudioDurationSeconds,
                            audioFileNameWithExt = audio.audioFileName,
                            imageFileNameWithExt = audio.imageFileName,
                            lyricFileNameWithExt = audio.lyricFileName,
                            tjKaraokeNumber = audio.tjKaraokeNumber,
                            editedAudioFileNameWithExt = audio.editedAudioFileName,
                            editedAudioDurationSeconds = audio.editedAudioDurationSeconds,
                            rawCreatedTime = audio.createdTimeMillis,
                        ).clampText()
                    ).toInt()
                }

                var nextSequence = playlistDao.getMaxSequence() + 1
                val playlistIdBySourceId = plan.newPlaylists.associate { playlist ->
                    playlist.id to playlistDao.insertPlaylist(
                        PlaylistEntity(
                            name = playlist.name,
                            description = playlist.description,
                            audioMediaCount = 0,
                            rawPlayTimeDuration = 0,
                            rawCreatedTime = playlist.createdTimeMillis,
                            imageFileNameWithExt = playlist.customImageFileName,
                            isCustomImage = playlist.customImageFileName != null,
                            sequence = nextSequence++,
                            type = playlist.type.name,
                        ).clampText()
                    ).toInt()
                }

                fun MigrationRef.resolve(idBySourceId: Map<Int, Int>) = when (this) {
                    is MigrationRef.Existing -> id
                    is MigrationRef.Incoming -> idBySourceId.getValue(sourceId)
                }

                val addedAudioMediaIds = mutableSetOf<Int>()
                plan.trackAdditions.forEach { addition ->
                    val audioMediaIdList = addition.audioMedias.map { it.resolve(audioIdBySourceId) }
                    addTrackWithSyncPlaylist(
                        playlistId = addition.playlist.resolve(playlistIdBySourceId),
                        audioMediaIdList = audioMediaIdList,
                        shouldInsertAtTop = false,
                    )
                    addedAudioMediaIds += audioMediaIdList
                }
                if (plan.uncategorizedRemovals.isNotEmpty()) {
                    deleteTrackWithSyncPlaylist(
                        playlistId = requireSystemPlaylistId(PlaylistType.UNCATEGORIZED),
                        audioMediaIdList = plan.uncategorizedRemovals,
                    )
                }
                // 이전·복원은 addTrack 을 거치지 않으므로 비밀 곡의 격리를 여기서 지킨다. 드러난
                // 플레이리스트에는 비밀 필터가 없어, 거기 남으면 비밀모드가 꺼져 있어도 보인다.
                val secretAudioMediaIds = addedAudioMediaIds.filter { isSecretAudioMedia(it) }
                if (secretAudioMediaIds.isNotEmpty()) unlinkFromOpenPlaylists(secretAudioMediaIds)

                plan.editProjects.forEach { project ->
                    audioEditProjectDao.upsert(
                        AudioEditProjectEntity(
                            audioMediaId = audioIdBySourceId.getValue(project.audioMediaId),
                            timelineJson = project.timelineJson,
                        )
                    )
                }
                stepDao.insertAllIgnoringExisting(
                    plan.steps.map { StepEntity(rawCreatedTime = it.createdTimeMillis, stepSensor = it.stepSensor) }
                )
                playlistIdBySourceId
            }
        }
    }

    suspend fun addNewAudioMedia(
        newAudioMedia: AudioMediaEntity,
        requestedPlaylistId: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): AddNewAudioMediaResult {
        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val totalId = requireSystemPlaylistId(PlaylistType.TOTAL)
                // 요청 대상이 실제 존재하면 그대로, 아니면 미분류로. 시스템 플레이리스트는 이 트랜잭션에서 보장 생성된다.
                val resolvedPlaylistId = resolveDownloadTargetId(requestedPlaylistId)
                val insertedAudioMediaId = audioMediaDao.insertAudioMedia(newAudioMedia).toInt()
                val totalPlaylist = addTrackWithSyncPlaylist(
                    playlistId = totalId,
                    audioMediaIdList = listOf(insertedAudioMediaId),
                    shouldInsertAtTop = shouldInsertAtTopOnDownload,
                )

                if (resolvedPlaylistId != totalId) {
                    addTrackWithSyncPlaylist(
                        playlistId = resolvedPlaylistId,
                        audioMediaIdList = listOf(insertedAudioMediaId),
                        shouldInsertAtTop = shouldInsertAtTopOnDownload,
                    )
                }
                AddNewAudioMediaResult(
                    audioMediaId = insertedAudioMediaId,
                    totalPlaylist = totalPlaylist,
                    totalPlaylistId = totalId,
                    resolvedPlaylistId = resolvedPlaylistId,
                )
            }
        }
    }

    private suspend fun resolveDownloadTargetId(requestedPlaylistId: Int?): Int {
        val type = requestedPlaylistId?.let { playlistDao.getTypeById(it) }
            ?.let { runCatching { PlaylistType.valueOf(it) }.getOrNull() }
        return if (type?.canBeDownloadTarget == true) requestedPlaylistId else requireSystemPlaylistId(PlaylistType.UNCATEGORIZED)
    }

    suspend fun updateAudioMedia(
        id: Int,
        name: String?,
        artist: String?,
        description: String?,
        lyricFileNameWithExt: String?,
    ) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                audioMediaDao.updateAudioMedia(
                    id = id,
                    name = name,
                    artist = artist,
                    description = description,
                    lyricFileNameWithExt = lyricFileNameWithExt,
                )
            }
        }
    }

    /** 사용자가 고른 썸네일을 입힌다. 정본은 그대로 남아 [clearCustomImage] 로 되돌릴 수 있다. */
    suspend fun applyCustomImage(audioMediaId: Int, customImageFileNameWithExt: String): List<String> =
        updateImageWithSyncPlaylist(audioMediaId) { before ->
            audioMediaDao.applyCustomImage(audioMediaId, customImageFileNameWithExt)
            listOfNotNull(before.customImageFileNameWithExt)
        }

    suspend fun clearCustomImage(audioMediaId: Int): List<String> =
        updateImageWithSyncPlaylist(audioMediaId) { before ->
            audioMediaDao.clearCustomImage(audioMediaId)
            listOfNotNull(before.customImageFileNameWithExt)
        }

    /** 정본을 다시 정한다. 옛 정본과 가리고 있던 선택이 함께 떨어져 나간다. */
    suspend fun replaceImage(audioMediaId: Int, imageFileNameWithExt: String): List<String> =
        updateImageWithSyncPlaylist(audioMediaId) { before ->
            audioMediaDao.replaceImage(audioMediaId, imageFileNameWithExt)
            listOfNotNull(before.imageFileNameWithExt, before.customImageFileNameWithExt)
        }

    /**
     * 플레이리스트 커버는 첫 곡의 썸네일에서 온다. 곡의 썸네일이 바뀌면 같은 트랜잭션에서 따라가야
     * 커버만 옛 그림으로 남지 않는다.
     *
     * 돌려주는 값은 이 변경으로 아무도 참조하지 않게 된 이미지 파일명들이다.
     */
    private suspend fun updateImageWithSyncPlaylist(
        audioMediaId: Int,
        block: suspend (before: AudioMediaEntity) -> List<String>,
    ): List<String> {
        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val orphanFileNames = block(audioMediaDao.getDataById(audioMediaId))
                playlistTrackDao.getLinkedPlaylistIdList(audioMediaId).forEach { playlistId ->
                    syncPlaylistWhenUpdateTrack(playlistId)
                }
                orphanFileNames
            }
        }
    }

    /**
     * 편집본을 음원에 연결한다. 재생 길이가 바뀌므로 플레이리스트 재생시간 합계까지 한 트랜잭션에서 보정한다.
     * 다시 편집할 수 있도록 편집 상태([timelineJson])도 같은 트랜잭션에서 저장한다.
     */
    suspend fun applyEditedAudio(
        audioMediaId: Int,
        editedAudioFileNameWithExt: String,
        editedAudioDurationSeconds: Long,
        timelineJson: String,
    ) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                updateDurationWithSyncPlaylist(
                    audioMediaId = audioMediaId,
                    newDurationSeconds = editedAudioDurationSeconds,
                ) {
                    audioMediaDao.applyEditedAudio(
                        id = audioMediaId,
                        editedAudioFileNameWithExt = editedAudioFileNameWithExt,
                        editedAudioDurationSeconds = editedAudioDurationSeconds,
                    )
                }
                audioEditProjectDao.upsert(
                    AudioEditProjectEntity(audioMediaId = audioMediaId, timelineJson = timelineJson)
                )
            }
        }
    }

    /** 편집본 연결을 해제해 원본으로 되돌린다. 길이도 원본 길이로 되돌아가고, 편집 상태도 지운다. */
    suspend fun clearEditedAudio(audioMediaId: Int) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val originDurationSeconds = audioMediaDao.getDataById(audioMediaId).rawAudioDurationSeconds ?: 0
                updateDurationWithSyncPlaylist(
                    audioMediaId = audioMediaId,
                    newDurationSeconds = originDurationSeconds,
                ) {
                    audioMediaDao.clearEditedAudio(id = audioMediaId)
                }
                audioEditProjectDao.deleteByAudioMediaId(audioMediaId)
            }
        }
    }

    /**
     * 음원의 실제 재생 길이가 바뀔 때 쓰는 공통 경로.
     *
     * 플레이리스트의 rawPlayTimeDuration은 트랙을 넣고 뺄 때마다 가감되는 누적값이고 재계산 경로가 없다.
     * 그래서 음원 길이가 바뀌면 그 차이만큼 직접 보정해줘야 한다.
     *
     * 기준값은 **반드시 [update] 전에** 읽는다. getTotalAudioDuration이 편집본 길이를 우선 집계하므로
     * update를 먼저 하면 바뀐 값이 조회되어 delta가 0이 된다.
     */
    private suspend fun updateDurationWithSyncPlaylist(
        audioMediaId: Int,
        newDurationSeconds: Long,
        update: suspend () -> Unit,
    ) {
        val oldDurationSeconds = audioMediaDao.getTotalAudioDuration(listOf(audioMediaId))
        update()

        val delta = newDurationSeconds - oldDurationSeconds
        if (delta == 0L) return

        playlistTrackDao.getLinkedPlaylistIdList(audioMediaId).forEach { playlistId ->
            val playlist = playlistDao.getPlaylistById(playlistId)
            playlistDao.updatePlaylist(
                id = playlistId,
                rawPlayTimeDuration = playlist.rawPlayTimeDuration + delta,
            )
        }
    }

    suspend fun deleteAudioMedia(audioMediaId: Int) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val playlistIdList = playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
                playlistIdList.forEach { linkedPlaylistId ->
                    deleteTrackWithSyncPlaylist(linkedPlaylistId, listOf(audioMediaId))
                }
                audioMediaDao.deleteById(audioMediaId)
            }
        }
    }

    suspend fun deleteOrphanAudioMedia(): List<AudioMediaEntity> {
        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val orphanList = audioMediaDao.getOrphanAudioMediaList()
                orphanList.forEach { orphan -> audioMediaDao.deleteById(orphan.id) }
                orphanList
            }
        }
    }

    suspend fun addPlaylist(title: String, type: PlaylistType): Int {
        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // 목록 맨 뒤에 오도록 현재 최대 sequence + 1을 부여한다.
                // MAX 조회와 insert를 한 트랜잭션으로 묶어 동시 추가 시 sequence 중복을 막는다.
                val nextSequence = playlistDao.getMaxSequence() + 1
                playlistDao.insertPlaylist(
                    PlaylistEntity(
                        name = title,
                        description = "",
                        audioMediaCount = 0,
                        rawPlayTimeDuration = 0,
                        isCustomImage = false,
                        sequence = nextSequence,
                        type = type.name,
                    ).clampText()
                ).toInt()
            }
        }
    }

    suspend fun deletePlaylist(playlistId: Int) {
        // 즐겨찾기를 포함한 시스템 플레이리스트는 지울 수 없다.
        if (playlistTypeOf(playlistId)?.isSystem == true) {
            error("시스템 플레이리스트는 지울 수 없다.")
        }

        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val linkedAudioMediaIdList = playlistTrackDao.getLinkedAudioMediaIdList(playlistId)
                playlistTrackDao.deletePlaylistTrackByPlaylistId(playlistId)
                syncUncategorizedPlaylistWhenDeleteTrack(linkedAudioMediaIdList)
                playlistDao.deleteById(playlistId)
                unlinkSecretFavoriteWhenNoLongerSecret(linkedAudioMediaIdList)
            }
        }
    }

    /** 걷어낸 사용자 썸네일의 파일 이름을 돌려준다. 고른 사진이 없었으면 null 이다. */
    suspend fun resetPlaylistImage(playlistId: Int): String? {
        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val playlist = playlistDao.getPlaylistById(playlistId)
                if (!playlist.isCustomImage) return@immediateTransaction null

                val firstTrackImage = suspendRunCatching {
                    playlistTrackDao.getFirstAudioMediaWithNoLock(playlistId)
                }.getOrNull()?.audioMediaEntity?.displayImageFileNameWithExt

                playlistDao.updatePlaylistImage(playlistId, firstTrackImage, isCustomImage = false)
                playlist.imageFileNameWithExt
            }
        }
    }

    /** 돌려주는 값은 비밀 플레이리스트에 담느라 다른 플레이리스트에서 빠진 곡 수다. */
    suspend fun addTrack(playlistId: Int, audioMediaIdList: List<Int>): Int {
        if (isAutoManagedPlaylist(playlistId)) {
            error("전체와 미분류는 추가할 수 없다.")
        }

        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // shouldInsertAtTop은 다운로드 시에만 사용하기 때문에 false 고정
                addTrackWithSyncPlaylist(playlistId, audioMediaIdList, shouldInsertAtTop = false)
                syncUncategorizedPlaylistWhenAddTrack(audioMediaIdList)
                if (playlistTypeOf(playlistId) == PlaylistType.SECRET) {
                    unlinkFromOpenPlaylists(audioMediaIdList)
                } else {
                    0
                }
            }
        }
    }

    /**
     * 자동으로 채워지는 곳(전체·미분류)은 건드리지 않고, 비밀 계열은 그대로 두고, 그 사이의
     * 드러난 플레이리스트만 끊는다. 미분류는 비밀에 담긴 순간 이미 빠졌다.
     *
     * 돌려주는 값은 한 군데라도 빠진 곡의 수다.
     */
    private suspend fun unlinkFromOpenPlaylists(audioMediaIdList: List<Int>): Int {
        // 플레이리스트별로 묶어 한 번에 뺀다. 곡마다 빼면 그 플레이리스트의 집계 동기화가 곡 수만큼 돈다.
        val audioMediaIdsByPlaylist = mutableMapOf<Int, MutableList<Int>>()
        val typeByPlaylist = mutableMapOf<Int, PlaylistType?>()
        audioMediaIdList.forEach { audioMediaId ->
            playlistTrackDao.getLinkedPlaylistIdList(audioMediaId).forEach { playlistId ->
                val type = typeByPlaylist.getOrPut(playlistId) { playlistTypeOf(playlistId) } ?: return@forEach
                if (type.isAutoManaged || type.isSecret) return@forEach
                audioMediaIdsByPlaylist.getOrPut(playlistId) { mutableListOf() }.add(audioMediaId)
            }
        }
        audioMediaIdsByPlaylist.forEach { (playlistId, ids) -> deleteTrackWithSyncPlaylist(playlistId, ids) }
        return audioMediaIdsByPlaylist.values.flatten().distinct().size
    }

    suspend fun deleteTrack(playlistId: Int, audioMediaIdList: List<Int>) {
        if (isAutoManagedPlaylist(playlistId)) {
            error("전체와 미분류는 지울 수 없다.")
        }
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                deleteTrackWithSyncPlaylist(playlistId, audioMediaIdList)
                syncUncategorizedPlaylistWhenDeleteTrack(audioMediaIdList)
                unlinkSecretFavoriteWhenNoLongerSecret(audioMediaIdList)
            }
        }
    }

    /** 비밀 플레이리스트에서 빠져 더 이상 비밀이 아닌 곡은 비밀 찜에도 남을 수 없다. */
    private suspend fun unlinkSecretFavoriteWhenNoLongerSecret(audioMediaIdList: List<Int>) {
        val secretFavoriteId = playlistDao.getPlaylistIdByType(PlaylistType.SECRET_FAVORITE.name) ?: return
        val linked = playlistTrackDao.getLinkedAudioMediaIdList(secretFavoriteId).toSet()
        // 실제로 담긴 것만 넘긴다 — 집계는 넘긴 개수만큼 빼므로 없는 곡을 섞으면 어긋난다.
        val toUnlink = audioMediaIdList.filter { it in linked && !isSecretAudioMedia(it) }
        if (toUnlink.isNotEmpty()) deleteTrackWithSyncPlaylist(secretFavoriteId, toUnlink)
    }

    private suspend fun isSecretAudioMedia(audioMediaId: Int): Boolean =
        playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
            .any { playlistTypeOf(it) == PlaylistType.SECRET }

    // 즐겨찾기 on/off. 미분류 동기화를 하지 않는 전용 경로(즐겨찾기는 분류와 직교).
    suspend fun setFavorite(audioMediaId: Int, isFavorite: Boolean) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // 비밀 곡의 찜은 일반 즐겨찾기와 섞이지 않게 다른 플레이리스트로 간다.
                val favoriteType =
                    if (isSecretAudioMedia(audioMediaId)) PlaylistType.SECRET_FAVORITE else PlaylistType.FAVORITE
                val favoriteId = requireSystemPlaylistId(favoriteType)
                val already = favoriteId in playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
                when {
                    isFavorite && !already ->
                        addTrackWithSyncPlaylist(favoriteId, listOf(audioMediaId), shouldInsertAtTop = false)

                    !isFavorite && already ->
                        deleteTrackWithSyncPlaylist(favoriteId, listOf(audioMediaId))
                }
            }
        }
    }

    // 시스템 플레이리스트가 없으면 만든다. 앱 시작 시 멱등 호출.
    suspend fun ensureSystemPlaylists() {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // 하나라도 빠지면 이전·복원이 통째로 실패한다 — 받는 쪽 계획이 시스템
                // 플레이리스트는 이미 있다고 보고 id 를 찾기 때문이다.
                PlaylistType.entries.filter { it.isSystem }.forEach { ensureSystemPlaylist(it) }
            }
        }
    }

    private suspend fun ensureSystemPlaylist(type: PlaylistType) {
        if (playlistDao.getPlaylistIdByType(type.name) != null) return
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = type.defaultName,
                description = "",
                audioMediaCount = 0,
                rawPlayTimeDuration = 0,
                isCustomImage = false,
                sequence = 0,
                type = type.name,
            ).clampText()
        )
    }

    private suspend fun isAutoManagedPlaylist(playlistId: Int): Boolean = playlistTypeOf(playlistId)?.isAutoManaged == true

    private suspend fun playlistTypeOf(playlistId: Int): PlaylistType? =
        playlistDao.getTypeById(playlistId)?.let { runCatching { PlaylistType.valueOf(it) }.getOrNull() }

    private suspend fun requireSystemPlaylistId(type: PlaylistType): Int {
        playlistDao.getPlaylistIdByType(type.name)?.let { return it }
        // 없으면(신규 설치 첫 접근 등) 같은 트랜잭션 안에서 즉시 생성해 보장한다.
        ensureSystemPlaylist(type)
        return playlistDao.getPlaylistIdByType(type.name)
            ?: error("${type.name} 플레이리스트 생성에 실패했습니다.")
    }

    // 전체/미분류/즐겨찾기 등 시스템 플레이리스트 id 집합. 미분류 orphan 판정에서 제외 대상.
    private suspend fun systemPlaylistIds(): Set<Int> {
        return PlaylistType.entries
            .filter { it.isSystem }
            .mapNotNull { playlistDao.getPlaylistIdByType(it.name) }
            .toSet()
    }

    suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                playlistTrackDao.updateSequence(playlistId, audioMediaId, oldSequence, newSequence)
                syncPlaylistWhenUpdateTrack(playlistId)
            }
        }
    }

    suspend fun reorderTracks(playlistId: Int, orderedAudioMediaIds: List<Int>) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                orderedAudioMediaIds.forEachIndexed { index, audioMediaId ->
                    playlistTrackDao.setSequence(playlistId, audioMediaId, sequence = index + 1)
                }
                syncPlaylistWhenUpdateTrack(playlistId)
            }
        }
    }

    suspend fun updatePlaylistSequences(orderedPlaylistIds: List<Int>) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                orderedPlaylistIds.forEachIndexed { index, id ->
                    // base playlist(sequence 0)보다 뒤로 배치되도록 1부터 부여
                    playlistDao.updateSequence(id = id, sequence = index + 1)
                }
            }
        }
    }

    private suspend fun addTrackWithSyncPlaylist(
        playlistId: Int,
        audioMediaIdList: List<Int>,
        shouldInsertAtTop: Boolean,
    ): PlaylistEntity {
        val targetList = if (shouldInsertAtTop) {
            playlistTrackDao.increaseSequenceAll(playlistId = playlistId, offset = audioMediaIdList.size)
            audioMediaIdList.withIndex().map { (index, audioMediaId) ->
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    audioMediaId = audioMediaId,
                    sequence = index + 1,
                )
            }
        } else {
            val nextSequence = playlistTrackDao.getMaxSequence(playlistId) + 1
            audioMediaIdList.withIndex().map { (index, audioMediaId) ->
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    audioMediaId = audioMediaId,
                    sequence = nextSequence + index,
                )
            }
        }

        playlistTrackDao.insertPlaylistTrackList(targetList)
        return syncPlaylistWhenAddTrack(
            playlistId = playlistId,
            audioMediaIdList = audioMediaIdList,
        )
    }

    private suspend fun deleteTrackWithSyncPlaylist(playlistId: Int, audioMediaIdList: List<Int>): PlaylistEntity {
        playlistTrackDao.deletePlaylistTracks(playlistId, audioMediaIdList)
        return syncPlaylistWhenDeleteTrack(
            playlistId = playlistId,
            audioMediaIdList = audioMediaIdList,
        )
    }

    private suspend fun syncPlaylistWhenAddTrack(
        playlistId: Int,
        audioMediaIdList: List<Int>
    ): PlaylistEntity {
        val firstAudioTrack = playlistTrackDao.getFirstAudioMediaWithNoLock(playlistId)
        val playlist = firstAudioTrack.playlistEntity

        val addTrackCount = audioMediaIdList.count()
        val addDuration = audioMediaDao.getTotalAudioDuration(audioMediaIdList)

        val newAudioMediaCount = playlist.audioMediaCount + addTrackCount
        val newPlayTimeDuration = playlist.rawPlayTimeDuration + addDuration
        val newImageFileNameWithExt = playlist.isCustomImage.takeUnless { it }?.let {
            firstAudioTrack.audioMediaEntity.displayImageFileNameWithExt
        }

        playlistDao.updatePlaylist(
            id = playlistId,
            audioMediaCount = newAudioMediaCount,
            rawPlayTimeDuration = newPlayTimeDuration,
            imageFileNameWithExt = newImageFileNameWithExt,
        )

        return playlist.copy(
            audioMediaCount = newAudioMediaCount,
            rawPlayTimeDuration = newPlayTimeDuration,
            imageFileNameWithExt = newImageFileNameWithExt ?: playlist.imageFileNameWithExt,
        )
    }

    private suspend fun syncPlaylistWhenDeleteTrack(
        playlistId: Int,
        audioMediaIdList: List<Int>,
    ): PlaylistEntity {
        val firstAudioTrack = suspendRunCatching {
            playlistTrackDao.getFirstAudioMediaWithNoLock(playlistId)
        }.getOrNull()

        val playlist = firstAudioTrack?.playlistEntity ?: playlistDao.getPlaylistById(playlistId)

        val deleteTrackCount = audioMediaIdList.count()
        val deleteDuration = audioMediaDao.getTotalAudioDuration(audioMediaIdList)

        val newAudioMediaCount = playlist.audioMediaCount - deleteTrackCount
        val newPlayTimeDuration = playlist.rawPlayTimeDuration - deleteDuration
        val newImageFileNameWithExt = playlist.isCustomImage.takeUnless { it }?.let {
            firstAudioTrack?.audioMediaEntity?.displayImageFileNameWithExt
        }

        playlistDao.updatePlaylist(
            id = playlistId,
            audioMediaCount = newAudioMediaCount,
            rawPlayTimeDuration = newPlayTimeDuration,
            imageFileNameWithExt = newImageFileNameWithExt,
        )

        val shouldDeleteImage = firstAudioTrack == null && !playlist.isCustomImage
        if (shouldDeleteImage) playlistDao.deletePlaylistImage(playlistId)

        return playlist.copy(
            audioMediaCount = newAudioMediaCount,
            rawPlayTimeDuration = newPlayTimeDuration,
            imageFileNameWithExt =
                if (shouldDeleteImage) null
                else newImageFileNameWithExt ?: playlist.imageFileNameWithExt,
        )
    }

    private suspend fun syncPlaylistWhenUpdateTrack(playlistId: Int) {
        val firstTrack = playlistTrackDao.getFirstAudioMediaWithNoLock(playlistId)

        val playListEntity = firstTrack.playlistEntity
        if (playListEntity.isCustomImage) return

        // 썸네일 없이 들어온 곡에서 고른 것을 떼면 커버도 비어야 한다. updatePlaylist 는 COALESCE 라
        // null 을 넘겨도 옛 파일명이 남고, 그 파일은 곧 지워져 커버가 없는 파일을 가리키게 된다.
        val newImageFileNameWithExt = firstTrack.audioMediaEntity.displayImageFileNameWithExt
        if (newImageFileNameWithExt == null) {
            playlistDao.deletePlaylistImage(playListEntity.id)
        } else {
            playlistDao.updatePlaylist(playListEntity.id, imageFileNameWithExt = newImageFileNameWithExt)
        }
    }

    private suspend fun syncUncategorizedPlaylistWhenAddTrack(audioMediaIdList: List<Int>) {
        val uncategorizedId = requireSystemPlaylistId(PlaylistType.UNCATEGORIZED)
        val targetAudioMediaIdList = mutableListOf<Int>()

        audioMediaIdList.forEach { audioMediaId ->
            val linkedPlaylistIdList = playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
            if (uncategorizedId in linkedPlaylistIdList) {
                targetAudioMediaIdList += audioMediaId
            }
        }

        if (targetAudioMediaIdList.isEmpty()) return
        deleteTrackWithSyncPlaylist(uncategorizedId, targetAudioMediaIdList)
    }

    private suspend fun syncUncategorizedPlaylistWhenDeleteTrack(audioMediaIdList: List<Int>) {
        // 전체/미분류/즐겨찾기는 "분류"로 치지 않는다(즐겨찾기는 미분류와 직교).
        // USER 플레이리스트가 하나도 남지 않았으면 미분류로 되돌린다.
        val systemIds = systemPlaylistIds()
        val targetAudioMediaIdList = mutableListOf<Int>()

        audioMediaIdList.forEach { audioMediaId ->
            val linked = playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
            if (linked.none { it !in systemIds }) {
                targetAudioMediaIdList += audioMediaId
            }
        }

        if (targetAudioMediaIdList.isEmpty()) return

        val uncategorizedId = requireSystemPlaylistId(PlaylistType.UNCATEGORIZED)
        // shouldInsertAtTop은 다운로드 시에만 사용하기 때문에 false 고정
        addTrackWithSyncPlaylist(
            playlistId = uncategorizedId,
            audioMediaIdList = targetAudioMediaIdList,
            shouldInsertAtTop = false,
        )
    }
}

data class AddNewAudioMediaResult(
    val audioMediaId: Int,
    val totalPlaylist: PlaylistEntity,
    val totalPlaylistId: Int,
    val resolvedPlaylistId: Int,
)
