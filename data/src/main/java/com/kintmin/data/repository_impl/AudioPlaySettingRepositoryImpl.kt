package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.preference_key.BooleanPreferenceKey
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AudioPlaySettingRepositoryImpl @Inject constructor(
    private val datastoreUtil: DatastoreUtil,
) : AudioPlaySettingRepository {
    override fun getIsPlaybackRepeatingFlow(): Flow<Boolean> {
        return datastoreUtil.isPlaybackRepeatingFlow
    }

    override fun getIsPlaybackShufflingFlow(): Flow<Boolean> {
        return datastoreUtil.isPlaybackShufflingFlow
    }

    override suspend fun updateIsPlaybackShuffling(isShuffling: Boolean): Result<Unit> {
        return datastoreUtil.updateBooleanData(BooleanPreferenceKey.IsPlaybackShuffling, isShuffling)
    }

    override suspend fun updateIsPlaybackRepeating(isRepeating: Boolean): Result<Unit> {
        return datastoreUtil.updateBooleanData(BooleanPreferenceKey.IsPlaybackRepeating, isRepeating)
    }
}