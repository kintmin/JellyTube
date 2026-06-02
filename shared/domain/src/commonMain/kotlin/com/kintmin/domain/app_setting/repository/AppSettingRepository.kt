package com.kintmin.domain.app_setting.repository

import kotlinx.coroutines.flow.Flow

interface AppSettingRepository {
    fun getShouldInsertAtTopOnDownloadFlow(): Flow<Boolean>
    fun getPlaylistIdOnDownloadFlow(): Flow<Int>
    fun getIsStepEnabledFlow(): Flow<Boolean>
    suspend fun updateShouldInsertAtTopOnDownload(value: Boolean): Result<Unit>
    suspend fun updatePlaylistIdOnDownload(playlistId: Int): Result<Unit>
    suspend fun updateIsStepEnabled(value: Boolean): Result<Unit>
}
