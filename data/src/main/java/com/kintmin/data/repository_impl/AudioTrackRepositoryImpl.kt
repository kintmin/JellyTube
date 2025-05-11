package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class AudioTrackRepositoryImpl @Inject constructor(
    private val playlistTrackDao: PlaylistTrackDao,
    private val fileManager: FileManager,
) : AudioTrackRepository {
    override suspend fun addAudioTrack(playlistId: Int, audioMediaId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val sequence = playlistTrackDao.getNextSequence(playlistId)

                playlistTrackDao.insertPlaylistTrack(
                    PlaylistTrackEntity(
                        playlistId = playlistId,
                        audioMediaId = audioMediaId,
                        sequence = sequence,
                        rawCreatedTime = Instant.now().toEpochMilli(),
                    )
                )
            }
        }
    }

    override suspend fun addAudioTrackList(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                var sequence = playlistTrackDao.getNextSequence(playlistId)
                val targetList = audioMediaIdList.map { audioMediaId ->
                    PlaylistTrackEntity(
                        playlistId = playlistId,
                        audioMediaId = audioMediaId,
                        sequence = sequence++,
                        rawCreatedTime = Instant.now().toEpochMilli(),
                    )
                }

                playlistTrackDao.insertPlaylistTrackList(targetList)
            }
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

    override suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, newSequence: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                playlistTrackDao.updateSequence(playlistId, audioMediaId, newSequence)
            }
        }
    }

    override suspend fun deleteAudioTrackList(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                playlistTrackDao.deletePlaylistTracks(playlistId, audioMediaIdList)
            }
        }
    }
}
