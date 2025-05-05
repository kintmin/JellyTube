package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    val playlistDao: PlaylistDao,
) : PlaylistRepository {
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

    override suspend fun updatePlaylistName(id: Int, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                playlistDao.updatePlaylistName(id, newName)
            }
        }


    override suspend fun updatePlaylistDescription(id: Int, newDescription: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                playlistDao.updatePlaylistDescription(id, newDescription)
            }
        }

    override suspend fun deletePlaylist(id: Int): Result<Unit> {
        TODO("Not yet implemented")
    }
}