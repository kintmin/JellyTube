package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AudioPlaySettingRepositoryImpl @Inject constructor(
    private val datastoreUtil: DatastoreUtil,
) : AudioPlaySettingRepository {

    override fun getIsPlaybackRepeatingFlow(): Flow<Boolean> {
        return datastoreUtil.getData(PreferencesKey.IsPlaybackRepeating).map {
            it ?: false
        }
    }

    override fun getIsPlaybackShufflingFlow(): Flow<Boolean> {
        return datastoreUtil.getData(PreferencesKey.IsPlaybackShuffling).map {
            it ?: false
        }
    }

    override suspend fun updateIsPlaybackRepeating(isRepeating: Boolean): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.IsPlaybackRepeating, isRepeating)
    }

    override suspend fun updateIsPlaybackShuffling(isShuffling: Boolean): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.IsPlaybackShuffling, isShuffling)
    }
}