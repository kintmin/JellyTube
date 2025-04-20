package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dataSource.LocalAudioDataSource
import com.kintmin.data.local_db.toDomain
import com.kintmin.data.local_db.toEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.model.AudioMediaData
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

internal class AudioMediaRepositoryImpl @Inject constructor(
    private val localAudioDataSource: LocalAudioDataSource,
    private val httpDataSource: HttpDataSource,
    private val fileManager: FileManager,
    private val pythonExecutor: PythonExecutor,
) : AudioMediaRepository {

    override suspend fun getListDataAll(): Result<List<AudioMediaData>> {
        return localAudioDataSource.getEntityListAll().mapCatching { entityList ->
            entityList.mapNotNull { entity -> entity.toDomain(fileManager).getOrNull() }
        }
    }

    override suspend fun getLocalData(id: String): Result<AudioMediaData> = runCatching {
        val entity = localAudioDataSource.getEntity(id).getOrThrow()
        entity.toDomain(fileManager).getOrThrow()
    }

    override suspend fun downloadData(
        downloadUrl: String,
        id: String,
    ): Result<AudioMediaData> = runCatching {
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

    override suspend fun saveDataToLocal(data: AudioMediaData): Result<Unit> = runCatching {
        localAudioDataSource.insertEntity(
            data.toEntity(fileManager)
        )
    }

    override suspend fun deleteMediaFile(id: String): Result<Unit> {
        var result = Result.success(Unit)
        coroutineScope {
            listOf(Ext.MP3, Ext.WEBP, Ext.JPG).map { ext ->
                async {
                    runCatching {
                        fileManager.deleteFile(id, ext)
                    }.onFailure {
                        result = Result.failure(it)
                    }
                }
            }.awaitAll()
        }
        return result
    }

    override suspend fun clearFileCache(): Result<Unit> {
        return fileManager.clearDiskCache()
    }
}