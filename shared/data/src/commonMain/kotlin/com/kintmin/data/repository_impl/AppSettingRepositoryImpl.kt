package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.playlist.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingRepositoryImpl constructor(
    private val datastoreUtil: DatastoreUtil,
) : AppSettingRepository {

    override fun getShouldInsertAtTopOnDownloadFlow(): Flow<Boolean> {
        return datastoreUtil.getData(PreferencesKey.ShouldInsertAtTopOnDownload).map {
            it ?: false
        }
    }

    override fun getPlaylistIdOnDownloadFlow(): Flow<Int> {
        return datastoreUtil.getData(PreferencesKey.PlaylistIdOnDownload).map {
            it ?: Playlist.UNCATEGORIZED
        }
    }

    override suspend fun updateShouldInsertAtTopOnDownload(value: Boolean): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.ShouldInsertAtTopOnDownload, value)
    }

    override suspend fun updatePlaylistIdOnDownload(playlistId: Int): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.PlaylistIdOnDownload, playlistId)
    }

    override fun getIsStepEnabledFlow(): Flow<Boolean> {
        return datastoreUtil.getData(PreferencesKey.IsStepEnabled).map {
            it ?: false
        }
    }

    override suspend fun updateIsStepEnabled(value: Boolean): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.IsStepEnabled, value)
    }
}
