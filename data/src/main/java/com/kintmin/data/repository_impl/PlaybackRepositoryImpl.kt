package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.preference_key.BooleanPreferenceKey
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlaybackRepositoryImpl @Inject constructor(
    private val datastoreUtil: DatastoreUtil,
    private val playlistTrackDao: PlaylistTrackDao,
) : PlaybackRepository {
    override fun getIsPlaybackRepeatingFlow(): Flow<Boolean> {
        return datastoreUtil.isPlaybackRepeatingFlow
    }

    override fun getIsPlaybackShufflingFlow(): Flow<Boolean> {
        return datastoreUtil.isPlaybackShufflingFlow
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
}
