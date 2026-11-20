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
import kotlin.time.Clock

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

    override suspend fun setCustomPlaylistImage(id: Int, imageData: ByteArray): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                val previous = playlistDao.getPlaylistById(id)
                // 같은 이름을 덮어쓰면 이미지 캐시가 경로를 키로 써서 옛 사진이 남는다.
                val fileName = "playlist_${id}_${Clock.System.now().toEpochMilliseconds()}"
                val ext = fileManager.saveImageWithCompression(imageData, fileName).getOrThrow()
                playlistDao.updatePlaylistImage(id, "$fileName.$ext", isCustomImage = true)

                if (previous.isCustomImage) deleteImageFile(previous.imageFileNameWithExt)
            }
        }
    }

    override suspend fun resetPlaylistImage(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                deleteImageFile(audioMediaFacade.resetPlaylistImage(id))
            }
        }
    }

    override suspend fun deletePlaylist(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                val playlist = playlistDao.getPlaylistById(id)
                audioMediaFacade.deletePlaylist(id)
                if (playlist.isCustomImage) deleteImageFile(playlist.imageFileNameWithExt)
            }
        }
    }

    // 파일이 남아도 이상 데이터 정리가 거둬 가므로 지우기 실패로 작업 전체를 실패시키지 않는다.
    private suspend fun deleteImageFile(imageFileNameWithExt: String?) {
        imageFileNameWithExt ?: return
        fileManager.getImageFileFullPath(imageFileNameWithExt)
            .onSuccess { fileManager.deleteFileAtFullPath(it) }
    }

    override suspend fun updatePlaylistSequences(orderedPlaylistIds: List<Int>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                audioMediaFacade.updatePlaylistSequences(orderedPlaylistIds)
            }
        }
    }
}