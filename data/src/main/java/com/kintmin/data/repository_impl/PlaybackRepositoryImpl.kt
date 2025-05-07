package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.preference_key.BooleanPreferenceKey
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class PlaybackRepositoryImpl @Inject constructor(
    private val datastoreUtil: DatastoreUtil,
    private val playlistTrackDao: PlaylistTrackDao,
) : PlaybackRepository {
    override suspend fun addAudioMediaListToPlaylist(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                var sequence = playlistTrackDao.getNextSequence(playlistId)
                val targetList = audioMediaIdList.map { audioMediaId ->
                    PlaylistTrackEntity(
                        playlistId = playlistId,
                        audioMediaId = audioMediaId,
                        sequence = sequence++,
                        rawCreatedTime = Instant.now().toEpochMilli(),
                    )
                }

                playlistTrackDao.insertPlaylistTrackList(targetList)
            }
        }
    }

    override fun getIsPlaybackRepeatingFlow(): Flow<Boolean> {
        return datastoreUtil.isPlaybackRepeatingFlow
    }

    override fun getIsPlaybackShufflingFlow(): Flow<Boolean> {
        return datastoreUtil.isPlaybackShufflingFlow
    }

    override suspend fun getAudioMediaIdList(playlistId: Int): Result<List<Int>> = withContext(Dispatchers.IO) {
        runCatching {
            playlistTrackDao.getAudioMediaIdList(playlistId)
        }
    }

    override suspend fun getPlaylistIdList(audioMediaId: Int): Result<List<Int>> = withContext(Dispatchers.IO) {
        runCatching {
            playlistTrackDao.getPlaylistIdList(audioMediaId)
        }
    }

    override suspend fun setIsPlaybackShuffling(isShuffling: Boolean): Result<Unit> {
        return datastoreUtil.updateBooleanData(BooleanPreferenceKey.IsPlaybackShuffling, isShuffling)
    }

    override suspend fun setPlaybackRepeating(isRepeating: Boolean): Result<Unit> {
        return datastoreUtil.updateBooleanData(BooleanPreferenceKey.IsPlaybackRepeating, isRepeating)
    }

    override suspend fun updatePlaybackSequence(
        playlistId: Int,
        audioMediaId: Int,
        newSequence: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            playlistTrackDao.updateSequence(playlistId, audioMediaId, newSequence)
        }
    }

    override suspend fun deletePlaylistTrackMedia(playlistId: Int, audioMediaId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                playlistTrackDao.deletePlaylistTrackMedia(playlistId, audioMediaId)
            }
        }
    }
}
