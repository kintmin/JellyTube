package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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

    override fun getPlaybackSpeedFlow(): Flow<Float> {
        return datastoreUtil.getData(PreferencesKey.PlaybackSpeed).map {
            it ?: 1.0f
        }
    }

    override fun getPlaybackPitchSemitoneFlow(): Flow<Int> {
        return datastoreUtil.getData(PreferencesKey.PlaybackPitchSemitone).map {
            it ?: 0
        }
    }

    override suspend fun updateIsPlaybackRepeating(isRepeating: Boolean): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.IsPlaybackRepeating, isRepeating)
    }

    override suspend fun updateIsPlaybackShuffling(isShuffling: Boolean): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.IsPlaybackShuffling, isShuffling)
    }

    override suspend fun updatePlaybackSpeed(speed: Float): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.PlaybackSpeed, speed)
    }

    override suspend fun updatePlaybackPitchSemitone(semitone: Int): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.PlaybackPitchSemitone, semitone)
    }
}