package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.preference_key.BooleanPreferenceKey
import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlaybackRepositoryImpl @Inject constructor(
    private val datastoreUtil: DatastoreUtil,
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
}
