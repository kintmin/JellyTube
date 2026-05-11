package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioTrackRepositoryImpl @Inject constructor(
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
        return playlistTrackDao.getPlaylistTrackFullFlow(playlistId, audioMediaId).map {
            it.toDomain(fileManager).getOrThrow()
        }
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
