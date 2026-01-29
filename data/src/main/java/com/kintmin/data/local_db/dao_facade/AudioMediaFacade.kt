package com.kintmin.data.local_db.dao_facade

import androidx.room.withTransaction
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.domain.playlist.model.Playlist
import javax.inject.Inject

class AudioMediaFacade @Inject constructor(
    private val db: JellyTubeDatabase,
    private val audioMediaDao: AudioMediaDao,
    private val playlistDao: PlaylistDao,
    private val playlistTrackDao: PlaylistTrackDao,
) {

    suspend fun addNewAudioMedia(newAudioMedia: AudioMediaEntity): Pair<Int, PlaylistEntity> {
        return db.withTransaction {
            val insertedAudioMediaId = audioMediaDao.insertAudioMedia(newAudioMedia).toInt()
            val totalPlaylist = addTrackWithSyncPlaylist(Playlist.TOTAL, listOf(insertedAudioMediaId))
            addTrackWithSyncPlaylist(Playlist.UNCATEGORIZED, listOf(insertedAudioMediaId))
            insertedAudioMediaId to totalPlaylist
        }
    }

    suspend fun deleteAudioMedia(audioMediaId: Int) {
        db.withTransaction {
            val playlistIdList = playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
            playlistIdList.forEach { linkedPlaylistId ->
                deleteTrackWithSyncPlaylist(linkedPlaylistId, listOf(audioMediaId))
            }
            audioMediaDao.deleteById(audioMediaId)
        }
    }

    suspend fun deletePlaylist(playlistId: Int) {
        if (playlistId == Playlist.TOTAL || playlistId == Playlist.UNCATEGORIZED) {
            error("전체나 미분류는 지울 수 없다.")
        }
        db.withTransaction {
            val linkedAudioMediaIdList = playlistTrackDao.getLinkedAudioMediaIdList(playlistId)
            playlistTrackDao.deletePlaylistTrackByPlaylistId(playlistId)
            syncUncategorizedPlaylistWhenDeleteTrack(linkedAudioMediaIdList)
            playlistDao.deleteById(playlistId)
        }
    }

    suspend fun addTrack(playlistId: Int, audioMediaIdList: List<Int>): PlaylistEntity {
        if (playlistId == Playlist.TOTAL || playlistId == Playlist.UNCATEGORIZED) {
            error("전체나 미분류는 추가할 수 없다.")
        }
        return db.withTransaction {
            val result = addTrackWithSyncPlaylist(playlistId, audioMediaIdList)
            syncUncategorizedPlaylistWhenAddTrack(audioMediaIdList)
            result
        }
    }

    suspend fun deleteTrack(playlistId: Int, audioMediaIdList: List<Int>) {
        if (playlistId == Playlist.TOTAL || playlistId == Playlist.UNCATEGORIZED) {
            error("전체나 미분류는 지울 수 없다.")
        }
        db.withTransaction {
            deleteTrackWithSyncPlaylist(playlistId, audioMediaIdList)
            syncUncategorizedPlaylistWhenDeleteTrack(audioMediaIdList)
        }
    }

    suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int) {
        db.withTransaction {
            playlistTrackDao.updateSequence(playlistId, audioMediaId, oldSequence, newSequence)
            syncPlaylistWhenUpdateTrack(playlistId)
        }
    }

    private suspend fun addTrackWithSyncPlaylist(playlistId: Int, audioMediaIdList: List<Int>): PlaylistEntity {
        val nextSequence = playlistTrackDao.getMaxSequence(playlistId) + 1
        val targetList = audioMediaIdList.withIndex().map { (index, audioMediaId) ->
            PlaylistTrackEntity(
                playlistId = playlistId,
                audioMediaId = audioMediaId,
                sequence = nextSequence + index,
            )
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
        val targetAudioMediaIdList = mutableListOf<Int>()

        audioMediaIdList.forEach { audioMediaId ->
            val linkedPlaylistIdList = playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
            if (Playlist.UNCATEGORIZED in linkedPlaylistIdList) {
                targetAudioMediaIdList += audioMediaId
            }
        }

        deleteTrackWithSyncPlaylist(Playlist.UNCATEGORIZED, targetAudioMediaIdList)
    }

    private suspend fun syncUncategorizedPlaylistWhenDeleteTrack(audioMediaIdList: List<Int>) {
        val targetAudioMediaIdList = mutableListOf<Int>()

        audioMediaIdList.forEach { audioMediaId ->
            val linkedPlaylistIdList = playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
            if (linkedPlaylistIdList.size == 1) {
                targetAudioMediaIdList += audioMediaId
            }
        }

        addTrackWithSyncPlaylist(Playlist.UNCATEGORIZED, targetAudioMediaIdList)
    }
}