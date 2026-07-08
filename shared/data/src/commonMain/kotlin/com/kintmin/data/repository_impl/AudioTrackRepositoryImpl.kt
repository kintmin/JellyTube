package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AudioTrackRepositoryImpl constructor(
    private val audioMediaFacade: AudioMediaFacade,
    private val playlistTrackDao: PlaylistTrackDao,
    private val fileManager: FileManager,
) : AudioTrackRepository {

    override suspend fun addCustomAudioTrack(
        playlistId: Int,
        audioMediaIdList: List<Int>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            audioMediaFacade.addTrack(playlistId, audioMediaIdList).audioMediaCount
        }
    }

    override suspend fun deleteCustomAudioTrack(
        playlistId: Int,
        audioMediaIdList: List<Int>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            audioMediaFacade.deleteTrack(playlistId, audioMediaIdList)
        }
    }

    override fun getPlaylistTrackAggregateFlow(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackAggregate> {
        // 관찰 도중 해당 트랙이 플레이리스트에서 제거되면 쿼리가 0행을 반환한다.
        // 이 삭제 순간의 null emission 은 흘려보낸다 (flatMapLatest 가 곧 재시작).
        return playlistTrackDao.getPlaylistTrackFullFlow(playlistId, audioMediaId)
            .filterNotNull()
            .map { it.toDomain(fileManager).getOrThrow() }
    }

    override fun getPlaylistTrackAggregateListFlow(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        return playlistTrackDao.getPlaylistTrackFullListFlow(playlistId).map { list ->
            list.map {
                it.toDomain(fileManager).getOrThrow()
            }
        }
    }

    override fun getPlaylistIdListFlow(audioMediaId: Int): Flow<List<Int>> {
        return playlistTrackDao.getPlaylistIdListFlow(audioMediaId)
    }

    override suspend fun setFavorite(audioMediaId: Int, isFavorite: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                audioMediaFacade.setFavorite(audioMediaId, isFavorite)
            }
        }

    override suspend fun getPlaylistTrackCount(playlistId: Int): Result<Int> {
        return withContext(Dispatchers.IO) {
            runCatching {
                playlistTrackDao.getPlaylistTrackCount(playlistId)
            }
        }
    }


    override suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                audioMediaFacade.updateTrackSequence(playlistId, audioMediaId, oldSequence, newSequence)
            }
        }
    }
}
