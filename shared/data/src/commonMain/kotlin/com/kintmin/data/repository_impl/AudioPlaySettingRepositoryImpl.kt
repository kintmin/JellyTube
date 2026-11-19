package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AudioPlaySettingRepositoryImpl constructor(
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

    override fun getSleepTimerMinutesFlow(): Flow<Int> {
        return datastoreUtil.getData(PreferencesKey.SleepTimerMinutes).map {
            it ?: DEFAULT_SLEEP_TIMER_MINUTES
        }
    }

    override fun getSleepTimerAnnouncementMediaIdFlow(): Flow<Int?> {
        return datastoreUtil.getData(PreferencesKey.SleepTimerAnnouncementMediaId).map {
            it?.takeUnless { mediaId -> mediaId == PreferencesKey.SleepTimerAnnouncementMediaId.NONE }
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

    override suspend fun updateSleepTimerMinutes(minutes: Int): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.SleepTimerMinutes, minutes)
    }

    override suspend fun updateSleepTimerAnnouncementMediaId(mediaId: Int?): Result<Unit> {
        return datastoreUtil.updateData(
            PreferencesKey.SleepTimerAnnouncementMediaId,
            mediaId ?: PreferencesKey.SleepTimerAnnouncementMediaId.NONE,
        )
    }

    private companion object {

        const val DEFAULT_SLEEP_TIMER_MINUTES = 30
    }
}