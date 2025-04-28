package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    val playlistDao: PlaylistDao,
): PlaylistRepository {
    override suspend fun addPlaylist(newValue: Playlist): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun getPlaylistListFlow(): Flow<List<Playlist>> {
        return playlistDao.getPlaylistListFlow().map { playlistList ->
            playlistList.map {
                it.toDomain()
            }
        }
    }

    override fun getPlaylistFlow(playlistId: Int): Flow<Playlist> {
        return playlistDao.getPlaylistFlow(playlistId).map { playlist ->
            playlist.toDomain()
        }
    }

    override suspend fun updatePlaylist(id: Long, updateValue: Playlist): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deletePlaylist(id: Long): Result<Unit> {
        TODO("Not yet implemented")
    }

}