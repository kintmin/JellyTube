package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistTrackDao: PlaylistTrackDao,
    private val fileManager: FileManager,
) : PlaylistRepository {

    override suspend fun addPlaylist(title: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = title,
                    description = "",
                    audioMediaCount = 0,
                    rawPlayTimeDuration = 0,
                    rawCreatedTime = Instant.now().toEpochMilli(),
                    isCustomImage = false,
                )
            ).toInt()
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
        return playlistDao.getPlaylistFlow(playlistId).map { playlist ->
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
            runCatching {
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
            runCatching {
                // 외래키 때문에 순차 삭제
                playlistTrackDao.deletePlaylistTrack(id)
                playlistDao.deletePlaylistName(id)
            }
        }
    }
}