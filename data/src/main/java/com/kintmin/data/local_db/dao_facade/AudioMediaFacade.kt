package com.kintmin.data.local_db.dao_facade

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.domain.playlist.model.Playlist
import java.time.Instant
import javax.inject.Inject

class AudioMediaFacade @Inject constructor(
    private val audioMediaDao: AudioMediaDao,
    private val playlistDao: PlaylistDao,
    private val playlistTrackDao: PlaylistTrackDao,
) {

    suspend fun addAudioMediaToPlaylist(playlistId: Int, audioMediaIdList: List<Int>): Int {
        val previousAudioMediaIdList = playlistTrackDao.getAudioMediaIdList(playlistId)
        val previousTotalCount = previousAudioMediaIdList.count()
        val currentTotalCount = previousTotalCount + audioMediaIdList.count()
        val currentPlayTimeDuration =
            audioMediaDao.getTotalAudioDuration(previousAudioMediaIdList) +
                    audioMediaDao.getTotalAudioDuration(audioMediaIdList)

        // 각 Playlist track의 sequence는 1부터 시작하며, 미디어의 순서값과 동일하다.
        val targetList = audioMediaIdList.withIndex().map { (index, audioMediaId) ->
            PlaylistTrackEntity(
                playlistId = playlistId,
                audioMediaId = audioMediaId,
                sequence = (previousTotalCount + 1) + index,
                rawCreatedTime = Instant.now().toEpochMilli(),
            )
        }
        playlistTrackDao.insertPlaylistTrackList(targetList)

        syncPlaylistAfterModifyAudioTrack(
            playlistId = playlistId,
            currentTotalCount = currentTotalCount,
            currentPlayTimeDuration = currentPlayTimeDuration,
        )

        if (playlistId != Playlist.UNCATEGORIZED) {
            syncUncategorizedPlaylistTrack(audioMediaIdList)
        }

        return currentTotalCount
    }

    suspend fun deleteAudioMediaToPlaylist(playlistId: Int, audioMediaIdList: List<Int>): Int {
        val previousAudioMediaIdList = playlistTrackDao.getAudioMediaIdList(playlistId)
        val previousTotalCount = previousAudioMediaIdList.count()
        val currentTotalCount = previousTotalCount - audioMediaIdList.count()
        val currentPlayTimeDuration =
            audioMediaDao.getTotalAudioDuration(previousAudioMediaIdList) -
                    audioMediaDao.getTotalAudioDuration(audioMediaIdList)

        playlistTrackDao.deletePlaylistTracks(playlistId, audioMediaIdList)

        syncPlaylistAfterModifyAudioTrack(
            playlistId = playlistId,
            currentTotalCount = currentTotalCount,
            currentPlayTimeDuration = currentPlayTimeDuration,
        )

        if (playlistId != Playlist.UNCATEGORIZED) {
            syncUncategorizedPlaylistTrack(audioMediaIdList)
        }

        return currentTotalCount
    }

    suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, newSequence: Int) {
        playlistTrackDao.updateSequence(playlistId, audioMediaId, newSequence)
        updateImageFileIfShouldChange(playlistId)
    }

    /**
     * track 업데이트 후 미분류에 추가/삭제를 보장한다.
     */
    private suspend fun syncUncategorizedPlaylistTrack(
        audioMediaIdList: List<Int>,
    ) {
        val uncategorizedTarget = audioMediaIdList.map { audioMediaId ->
            val linkedPlaylistIdList = playlistTrackDao.getLinkedPlaylistIdList(audioMediaId)
            if (linkedPlaylistIdList.size == 1) {
                audioMediaId to null
            } else if (linkedPlaylistIdList.size > 2 && (Playlist.UNCATEGORIZED in linkedPlaylistIdList)) {
                null to audioMediaId
            } else {
                null to null
            }
        }

        uncategorizedTarget.mapNotNull { it.first }.takeIf { it.isNotEmpty() }?.let {
            addAudioMediaToPlaylist(Playlist.UNCATEGORIZED, it)
        }
        uncategorizedTarget.mapNotNull { it.second }.takeIf { it.isNotEmpty() }?.let {
            deleteAudioMediaToPlaylist(Playlist.UNCATEGORIZED, it)
        }
    }

    /**
     * track 업데이트 후 Playlist의 [미디어 개수], [플레이 시간], [이미지] 를 보장한다.
     */
    private suspend fun syncPlaylistAfterModifyAudioTrack(
        playlistId: Int,
        currentTotalCount: Int,
        currentPlayTimeDuration: Long,
    ) {
        playlistDao.updatePlaylist(
            id = playlistId,
            audioMediaCount = currentTotalCount,
            rawPlayTimeDuration = currentPlayTimeDuration,
        )

        updateImageFileIfShouldChange(playlistId)
    }

    private suspend fun updateImageFileIfShouldChange(playlistId: Int) {
        // Playlist에 커스텀한 이미지를 사용중이면 무시한다.
        val playListEntity = playlistDao.getPlaylistById(playlistId)
        if (playListEntity.isCustomImage) return

        val currentTotalCount = playlistTrackDao.getPlaylistTrackCount(playlistId)
        if (currentTotalCount == 0) {
            // 현재 Playlist에 연결된 미디어가 하나도 없다면 이미지를 삭제한다.
            playlistDao.deletePlaylistImage(playlistId)
        } else {
            // 현재 Playlist의 첫번째 미디어의 이미지를 Playlist 이미지로 지정한다.
            val firstMedia = playlistTrackDao.getFirstAudioMedia(playListEntity.id).audioMediaEntity
            playlistDao.updatePlaylist(playListEntity.id, imageFileNameWithExt = firstMedia.imageFileNameWithExt)
        }
    }
}