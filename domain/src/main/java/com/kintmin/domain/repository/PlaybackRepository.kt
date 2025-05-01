package com.kintmin.domain.repository

import kotlinx.coroutines.flow.Flow

interface PlaybackRepository {
    fun getIsPlaybackRepeatingFlow(): Flow<Boolean>
    fun getIsPlaybackShufflingFlow(): Flow<Boolean>
    suspend fun setIsPlaybackShuffling(isShuffling: Boolean): Result<Unit>
    suspend fun setPlaybackRepeating(isRepeating: Boolean): Result<Unit>
}