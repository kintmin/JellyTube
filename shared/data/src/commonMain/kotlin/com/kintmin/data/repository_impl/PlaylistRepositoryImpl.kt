package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistRepositoryImpl constructor(
    private val audioMediaFacade: AudioMediaFacade,
    private val playlistDao: PlaylistDao,
    private val fileManager: FileManager,
) : PlaylistRepository {

    override suspend fun ensureSystemPlaylists(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            audioMediaFacade.ensureSystemPlaylists()
        }
    }

    override suspend fun addPlaylist(title: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            audioMediaFacade.addPlaylist(title)
        }
    }

    override fun getAllPlaylistFlow(): Flow<List<Playlist>> {
        return playlistDao.getPlaylistListFlow().map { playlistList ->
            playlistList.map {
                it.toDomain(fileManager).getOrThrow()
            }
        }
    }

    override fun getPlaylistFlow(playlistId: Int): Flow<Playlist> {
        // 관찰 도중 플레이리스트가 삭제되면 쿼리가 0행을 반환한다.
        // 이 삭제 순간의 null emission 은 흘려보낸다 (화면은 곧 이탈).
        return playlistDao.getPlaylistFlow(playlistId)
            .filterNotNull()
            .map { playlist ->
                playlist.toDomain(fileManager).getOrThrow()
            }
    }

    override suspend fun updatePlaylist(
        id: Int,
        name: String?,
        description: String?,
        imageFileFullPath: String?,
        audioMediaCount: Int?,
        rawTotalDuration: Long?,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                playlistDao.updatePlaylist(
                    id = id,
                    name = name,
                    description = description,
                    imageFileNameWithExt = imageFileFullPath?.let {
                        fileManager.getFileNameWithExt(it).getOrThrow()
                    },
                    audioMediaCount = audioMediaCount,
                    rawPlayTimeDuration = rawTotalDuration,
                )
            }
        }
    }

    override suspend fun deletePlaylist(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                audioMediaFacade.deletePlaylist(id)
            }
        }
    }

    override suspend fun updatePlaylistSequences(orderedPlaylistIds: List<Int>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                audioMediaFacade.updatePlaylistSequences(orderedPlaylistIds)
            }
        }
    }
}