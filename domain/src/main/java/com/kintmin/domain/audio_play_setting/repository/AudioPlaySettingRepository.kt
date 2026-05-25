package com.kintmin.domain.audio_play_setting.repository

import kotlinx.coroutines.flow.Flow

interface AudioPlaySettingRepository {
    fun getIsPlaybackRepeatingFlow(): Flow<Boolean>
    fun getIsPlaybackShufflingFlow(): Flow<Boolean>
    fun getPlaybackSpeedFlow(): Flow<Float>
    fun getPlaybackPitchSemitoneFlow(): Flow<Int>
    suspend fun updateIsPlaybackRepeating(isRepeating: Boolean): Result<Unit>
    suspend fun updateIsPlaybackShuffling(isShuffling: Boolean): Result<Unit>
    suspend fun updatePlaybackSpeed(speed: Float): Result<Unit>
    suspend fun updatePlaybackPitchSemitone(semitone: Int): Result<Unit>
}