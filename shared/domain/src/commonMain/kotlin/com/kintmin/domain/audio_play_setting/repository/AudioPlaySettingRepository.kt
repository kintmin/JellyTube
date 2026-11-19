package com.kintmin.domain.audio_play_setting.repository

import kotlinx.coroutines.flow.Flow

interface AudioPlaySettingRepository {
    fun getIsPlaybackRepeatingFlow(): Flow<Boolean>
    fun getIsPlaybackShufflingFlow(): Flow<Boolean>
    fun getPlaybackSpeedFlow(): Flow<Float>
    fun getPlaybackPitchSemitoneFlow(): Flow<Int>
    fun getSleepTimerMinutesFlow(): Flow<Int>

    /** 잠들기 타이머의 마무리 음성으로 고른 음원. null 이면 고르지 않았다. */
    fun getSleepTimerAnnouncementMediaIdFlow(): Flow<Int?>
    suspend fun updateIsPlaybackRepeating(isRepeating: Boolean): Result<Unit>
    suspend fun updateIsPlaybackShuffling(isShuffling: Boolean): Result<Unit>
    suspend fun updatePlaybackSpeed(speed: Float): Result<Unit>
    suspend fun updatePlaybackPitchSemitone(semitone: Int): Result<Unit>
    suspend fun updateSleepTimerMinutes(minutes: Int): Result<Unit>
    suspend fun updateSleepTimerAnnouncementMediaId(mediaId: Int?): Result<Unit>
}