package com.kintmin.data.local_db.dao_facade

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.domain.playlist.model.PlaylistType

class AudioMediaFacade constructor(
    private val db: JellyTubeDatabase,
    private val audioMediaDao: AudioMediaDao,
    private val playlistDao: PlaylistDao,
    private val playlistTrackDao: PlaylistTrackDao,
) {

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

    // 다운로드 대상 id 확정. 요청 id가 실제 존재하면 사용, 아니면 미분류(없으면 생성)로 fallback.
    private suspend fun resolveDownloadTargetId(requestedPlaylistId: Int?): Int {
        val exists = requestedPlaylistId != null && playlistDao.getTypeById(requestedPlaylistId) != null
        return if (exists) requestedPlaylistId!! else requireSystemPlaylistId(PlaylistType.UNCATEGORIZED)
    }

    suspend fun updateAudioMedia(
        id: Int,
        name: String?,
        artist: String?,
        description: String?,
        imageFileNameWithExt: String?,
        lyricFileNameWithExt: String?,
    ) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                audioMediaDao.updateAudioMedia(
                    id = id,
                    name = name,
                    artist = artist,
                    description = description,
                    imageFileNameWithExt = imageFileNameWithExt,
                    lyricFileNameWithExt = lyricFileNameWithExt,
                )
                if (imageFileNameWithExt != null) {
                    playlistTrackDao.getLinkedPlaylistIdList(id).forEach { playlistId ->
                        syncPlaylistWhenUpdateTrack(playlistId)
                    }
                }
            }
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

    suspend fun addPlaylist(title: String): Int {
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
                    )
                ).toInt()
            }
        }
    }

    suspend fun deletePlaylist(playlistId: Int) {
        // 즐겨찾기를 포함한 시스템 플레이리스트는 지울 수 없다.
        val type = playlistDao.getTypeById(playlistId)
            ?.let { runCatching { PlaylistType.valueOf(it) }.getOrNull() }
        if (type?.isSystem == true) {
            error("시스템 플레이리스트는 지울 수 없다.")
        }

        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val linkedAudioMediaIdList = playlistTrackDao.getLinkedAudioMediaIdList(playlistId)
                playlistTrackDao.deletePlaylistTrackByPlaylistId(playlistId)
                syncUncategorizedPlaylistWhenDeleteTrack(linkedAudioMediaIdList)
                playlistDao.deleteById(playlistId)
            }
        }
    }

    suspend fun addTrack(playlistId: Int, audioMediaIdList: List<Int>): PlaylistEntity {
        if (isAutoManagedPlaylist(playlistId)) {
            error("전체와 미분류는 추가할 수 없다.")
        }

        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // shouldInsertAtTop은 다운로드 시에만 사용하기 때문에 false 고정
                val result = addTrackWithSyncPlaylist(playlistId, audioMediaIdList, shouldInsertAtTop = false)
                syncUncategorizedPlaylistWhenAddTrack(audioMediaIdList)
                result
            }
        }
    }

    suspend fun deleteTrack(playlistId: Int, audioMediaIdList: List<Int>) {
        if (isAutoManagedPlaylist(playlistId)) {
            error("전체와 미분류는 지울 수 없다.")
        }
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                deleteTrackWithSyncPlaylist(playlistId, audioMediaIdList)
                syncUncategorizedPlaylistWhenDeleteTrack(audioMediaIdList)
            }
        }
    }

    // 즐겨찾기 on/off. 미분류 동기화를 하지 않는 전용 경로(즐겨찾기는 분류와 직교).
    suspend fun setFavorite(audioMediaId: Int, isFavorite: Boolean) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val favoriteId = requireSystemPlaylistId(PlaylistType.FAVORITE)
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

    // 시스템 플레이리스트(전체/미분류/즐겨찾기)가 없으면 만든다. 앱 시작 시 멱등 호출.
    // id는 지정하지 않고(autoGenerate) type으로 구분한다.
    suspend fun ensureSystemPlaylists() {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                ensureSystemPlaylist(PlaylistType.TOTAL)
                ensureSystemPlaylist(PlaylistType.UNCATEGORIZED)
                ensureSystemPlaylist(PlaylistType.FAVORITE)
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
            )
        )
    }

    // 전체/미분류는 사용자가 직접 트랙을 추가/삭제할 수 없는 자동관리 플레이리스트다.
    private suspend fun isAutoManagedPlaylist(playlistId: Int): Boolean {
        val type = playlistDao.getTypeById(playlistId)
            ?.let { runCatching { PlaylistType.valueOf(it) }.getOrNull() }
        return type == PlaylistType.TOTAL || type == PlaylistType.UNCATEGORIZED
    }

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
            firstAudioTrack.audioMediaEntity.imageFileNameWithExt
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
        val firstAudioTrack = runCatching {
            playlistTrackDao.getFirstAudioMediaWithNoLock(playlistId)
        }.getOrNull()

        val playlist = firstAudioTrack?.playlistEntity ?: playlistDao.getPlaylistById(playlistId)

        val deleteTrackCount = audioMediaIdList.count()
        val deleteDuration = audioMediaDao.getTotalAudioDuration(audioMediaIdList)

        val newAudioMediaCount = playlist.audioMediaCount - deleteTrackCount
        val newPlayTimeDuration = playlist.rawPlayTimeDuration - deleteDuration
        val newImageFileNameWithExt = playlist.isCustomImage.takeUnless { it }?.let {
            firstAudioTrack?.audioMediaEntity?.imageFileNameWithExt
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

        val firstMedia = firstTrack.audioMediaEntity
        playlistDao.updatePlaylist(playListEntity.id, imageFileNameWithExt = firstMedia.imageFileNameWithExt)
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
