package com.kintmin.domain.app_setting.repository

import kotlinx.coroutines.flow.Flow

interface AppSettingRepository {
    fun getShouldInsertAtTopOnDownloadFlow(): Flow<Boolean>
    // 다운로드 대상 플레이리스트 id. 미설정 시 null(소비 측에서 미분류로 해석).
    fun getPlaylistIdOnDownloadFlow(): Flow<Int?>
    fun getIsStepEnabledFlow(): Flow<Boolean>
    suspend fun updateShouldInsertAtTopOnDownload(value: Boolean): Result<Unit>
    suspend fun updatePlaylistIdOnDownload(playlistId: Int): Result<Unit>
    suspend fun updateIsStepEnabled(value: Boolean): Result<Unit>

    /** 첫 화면 안내는 평생 한 번만 띄운다. 보여 준 뒤로는 설정에서만 다룬다. */
    fun getHasShownNotificationNoticeFlow(): Flow<Boolean>
    suspend fun markNotificationNoticeShown(): Result<Unit>
    fun getHasShownBackupNoticeFlow(): Flow<Boolean>
    suspend fun markBackupNoticeShown(): Result<Unit>
}
