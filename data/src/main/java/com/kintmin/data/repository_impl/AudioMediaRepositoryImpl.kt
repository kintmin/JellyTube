package com.kintmin.data.repository_impl

import androidx.paging.PagingData
import androidx.paging.map
import com.kintmin.data.local_db.dataSource.LocalAudioDataSource
import com.kintmin.data.local_db.toDomain
import com.kintmin.data.local_db.toEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

internal class AudioMediaRepositoryImpl @Inject constructor(
    private val localAudioDataSource: LocalAudioDataSource,
    private val httpDataSource: HttpDataSource,
    private val fileManager: FileManager,
    private val pythonExecutor: PythonExecutor,
) : AudioMediaRepository {

    override fun getPagingAudioMediaFlow(): Flow<PagingData<AudioMedia>> {
        return localAudioDataSource.getPagingEntityFlow().map { pagingData ->
            pagingData.map { audioEntity ->
                audioEntity.toDomain(fileManager)
            }
        }
    }

    override suspend fun getAudioMediaList(): Result<List<AudioMedia>> {
        return localAudioDataSource.getEntityList().mapCatching { listData ->
            listData.map { it.toDomain(fileManager) }
        }
    }

    override suspend fun getAudioMedia(id: String): Result<AudioMedia> {
        return localAudioDataSource.getEntity(id).mapCatching { it.toDomain(fileManager) }
    }

    override suspend fun downloadAudioMedia(
        downloadUrl: String,
        id: String,
    ): Result<AudioMedia> = runCatching {
        val audioFileFullPath = fileManager.getFullPathWithExt(
            fileName = id,
            ext = Ext.MP3,
        ).getOrThrow()

        val downloadDto = pythonExecutor.downloadYoutubeMedia(
            youtubeUrl = downloadUrl,
            audioDownloadPath = audioFileFullPath,
        ).getOrThrow()

        val imageFileFullPath = httpDataSource.downloadImage(
            imageUrl = downloadDto.thumbnailDownloadUrl
        ).getOrNull()?.let { image ->
            fileManager.saveImageWithCompression(image, id).getOrNull()
        }

        val createdTime = Instant.now()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        downloadDto.toDomain(
            id = id,
            createdTime = createdTime,
            audioFileFullPath = audioFileFullPath,
            imageFileFullPath = imageFileFullPath,
        )
    }

    override suspend fun updateAudioMedia(id: String, newAudioMedia: AudioMedia): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun addAudioMedia(newAudioMedia: AudioMedia): Result<Unit> {
        return localAudioDataSource.insertEntity(
            newAudioMedia.toEntity(fileManager)
        )
    }

    override suspend fun deleteAudioMediaInvalidCache(id: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                listOf(Ext.MP3, Ext.WEBP, Ext.JPG).map { ext ->
                    async { fileManager.deleteFile(id, ext) }
                }.awaitAll()
            }
        }
        fileManager.clearDiskCache()
    }

    override suspend fun deleteAudioMedia(id: String): Result<Unit> = runCatching {
        localAudioDataSource.deleteEntity(id).onSuccess {
            deleteAudioMediaInvalidCache(id).getOrThrow()
        }.getOrThrow()
    }
}