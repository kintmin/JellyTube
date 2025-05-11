package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

internal class AudioMediaRepositoryImpl @Inject constructor(
    private val audioMediaDao: AudioMediaDao,
    private val playlistTrackDao: PlaylistTrackDao,
    private val httpDataSource: HttpDataSource,
    private val fileManager: FileManager,
    private val pythonExecutor: PythonExecutor,
) : AudioMediaRepository {

    override suspend fun addAudioMedia(downloadUrl: String): Result<AudioMedia> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val fileName = UUID.randomUUID().toString()

                val audioFileFullPath = fileManager.getFullPathWithExt(
                    fileName = fileName,
                    ext = Ext.MP3,
                ).getOrThrow()

                val downloadDto = pythonExecutor.downloadYoutubeMedia(
                    youtubeUrl = downloadUrl,
                    audioDownloadPath = audioFileFullPath,
                ).getOrThrow()

                val imageFileExt = httpDataSource.downloadImage(
                    imageUrl = downloadDto.thumbnailDownloadUrl
                ).getOrNull()?.let { image ->
                    fileManager.saveImageWithCompression(
                        imageData = image,
                        fileName = fileName,
                    ).getOrNull()
                }

                val audioMediaEntityToSave = AudioMediaEntity(
                    source = downloadUrl,
                    name = downloadDto.title,
                    artist = downloadDto.uploader,
                    description = downloadDto.description,
                    rawAudioDurationSeconds = downloadDto.duration.toLongOrNull(),
                    audioFileNameWithExt = "${fileName}.${Ext.MP3}",
                    imageFileNameWithExt = imageFileExt?.let { "${fileName}.${it}" },
                    rawCreatedTime = Instant.now().toEpochMilli(),
                )

                val audioMediaId = audioMediaDao.insertAudioMedia(audioMediaEntityToSave).toInt()
                audioMediaEntityToSave.copy(id = audioMediaId).toDomain(fileManager).getOrThrow()
            }
        }
    }

    override suspend fun getAudioMediaBySource(source: String): Result<AudioMedia> {
        return withContext(Dispatchers.IO) {
            runCatching {
                audioMediaDao.getDataBySource(source).toDomain(fileManager).getOrThrow()
            }
        }
    }

    override suspend fun updateAudioMedia(
        id: Int,
        name: String?,
        artist: String?,
        description: String?,
        imageFileNameWithExt: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val data = audioMediaDao.getDataById(id)

                audioMediaDao.updateAudioMedia(
                    data.copy(
                        name = name ?: data.name,
                        artist = artist ?: data.artist,
                        description = description ?: data.description,
                        imageFileNameWithExt = imageFileNameWithExt ?: data.imageFileNameWithExt,
                    )
                )
            }
        }
    }

    override suspend fun deleteAudioMedia(id: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val data = audioMediaDao.getDataById(id)

            listOf(
                async { fileManager.deleteFile(data.audioFileNameWithExt) },
                async { fileManager.deleteFile(data.audioFileNameWithExt) },
                async {
                    // 외래키 때문에 순차 삭제
                    playlistTrackDao.deleteAudioMedia(id)
                    audioMediaDao.deleteById(id)
                }
            ).awaitAll()
        }
    }
}