package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.mapper.AudioMediaMapper
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.python_bridge.mapper.toDomain
import com.kintmin.domain.extension.toMillis
import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.model.DownloadedAudioMedia
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

internal class AudioMediaRepositoryImpl @Inject constructor(
    private val audioMediaDao: AudioMediaDao,
    private val playlistTrackDao: PlaylistTrackDao,
    private val httpDataSource: HttpDataSource,
    private val fileManager: FileManager,
    private val pythonExecutor: PythonExecutor,
) : AudioMediaRepository {

    override fun getAudioMediaListFlow(playlistId: Int): Flow<List<AudioMedia>> {
        return playlistTrackDao.getPlaylistTrackFullListFlow(playlistId).map { playlistTrackFull ->
            playlistTrackFull.mapNotNull {
                AudioMediaMapper.toDomain(
                    fileManager = fileManager,
                    audioMediaEntity = it.audioMediaEntity,
                    playlistTrackEntity = it.playlistTrackEntity,
                ).getOrNull()
            }.sortedBy { it.audioMediaSequence }
        }
    }

    override suspend fun getAudioMediaBySource(source: String): Result<AudioMedia> = runCatching {
        withContext(Dispatchers.IO) {
            val audioMediaEntity = audioMediaDao.getDataBySource(source)
            AudioMediaMapper.toDomain(
                fileManager = fileManager,
                audioMediaEntity = audioMediaEntity,
            ).getOrThrow()
        }
    }

    override suspend fun downloadAudioMedia(
        downloadUrl: String,
        source: String,
    ): Result<DownloadedAudioMedia> = runCatching {
        withContext(Dispatchers.IO) {
            val audioFileFullPath = fileManager.getFullPathWithExt(
                fileName = source,
                ext = Ext.MP3,
            ).getOrThrow()

            val downloadDto = pythonExecutor.downloadYoutubeMedia(
                youtubeUrl = downloadUrl,
                audioDownloadPath = audioFileFullPath,
            ).getOrThrow()

            val imageFileExt = httpDataSource.downloadImage(
                imageUrl = downloadDto.thumbnailDownloadUrl
            ).getOrNull()?.let { image ->
                fileManager.saveImageWithCompression(image, source).getOrNull()
            }

            downloadDto.toDomain(
                source = source,
                audioFileName = source,
                audioFileExtName = Ext.MP3.toString(),
                imageFileName = source,
                imageFileExtName = imageFileExt?.toString(),
            )
        }
    }

    override suspend fun addAudioMedia(newAudioMedia: DownloadedAudioMedia): Result<AudioMedia> = runCatching {
        withContext(Dispatchers.IO) {
            var audioMediaEntityToSave = AudioMediaEntity(
                source = newAudioMedia.source,
                mediaName = newAudioMedia.title,
                artist = newAudioMedia.uploader,
                description = newAudioMedia.description,
                rawAudioDurationSeconds = newAudioMedia.duration,
                audioFileExt = newAudioMedia.audioFileExtName,
                imageFileExt = newAudioMedia.imageFileExtName,
                rawCreatedTime = newAudioMedia.createdTime.toMillis(),
            )

            val audioMediaId = audioMediaDao.insert(audioMediaEntityToSave).toInt()
            audioMediaEntityToSave = audioMediaEntityToSave.copy(id = audioMediaId)

            val totalNextSequence = playlistTrackDao.getNextSequence(Playlist.TOTAL)
            val uncategorizedNextSequence = playlistTrackDao.getNextSequence(Playlist.UNCATEGORIZED)

            playlistTrackDao.insertPlaylistTrack(
                PlaylistTrackEntity(
                    playlistId = Playlist.TOTAL,
                    audioMediaId = audioMediaId,
                    sequence = totalNextSequence,
                    rawCreatedTime = newAudioMedia.createdTime.toMillis(),
                )
            )

            playlistTrackDao.insertPlaylistTrack(
                PlaylistTrackEntity(
                    playlistId = Playlist.UNCATEGORIZED,
                    audioMediaId = audioMediaId,
                    sequence = uncategorizedNextSequence,
                    rawCreatedTime = newAudioMedia.createdTime.toMillis(),
                )
            )

            AudioMediaMapper.toDomain(
                fileManager = fileManager,
                audioMediaEntity = audioMediaEntityToSave,
            ).getOrThrow()
        }
    }

    override suspend fun deleteInvalidAudioMediaFile(fileName: String): Result<Unit> = runCatching {
        withTimeout(3000L) {
            Ext.entries.map { ext ->
                async { fileManager.deleteFile(fileName, ext) }
            }.awaitAll()
        }
        fileManager.clearDiskCache()
    }

    override suspend fun deleteAudioMedia(id: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val fileName = audioMediaDao.getDataById(id).source
            Ext.entries.map { ext ->
                async { fileManager.deleteFile(fileName, ext) }
            }.awaitAll()
            playlistTrackDao.deleteAudioMedia(id)
            audioMediaDao.deleteById(id)
        }
    }
}