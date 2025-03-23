package com.kintmin.data.repositoryImpl

import com.kintmin.data.local.datasource.FileManager
import com.kintmin.data.local.datasource.YoutubeMediaDataSource
import com.kintmin.data.local.datasource.PythonExecutor
import com.kintmin.data.local.entity.AudioMediaEntity
import com.kintmin.data.remote.datasource.HttpDataSource
import com.kintmin.domain.repository.YoutubeMediaRepository
import com.kintmin.domain.model.AudioMediaData
import javax.inject.Inject

class YoutubeMediaRepositoryImpl @Inject constructor(
    private val youtubeMediaDataSource: YoutubeMediaDataSource,
    private val fileManager: FileManager,
    private val pythonExecutor: PythonExecutor,
    private val httpDataSource: HttpDataSource,
) : YoutubeMediaRepository {
    override suspend fun getMediaData(youtubeUrl: String, videoId: String): Result<AudioMediaData> =
        runCatching {
            val audioDownloadPath =
                fileManager.getFullPathWithExt(videoId, FileManager.Ext.MP3).getOrThrow()
            val dto =
                pythonExecutor.downloadYoutubeMedia(youtubeUrl, audioDownloadPath).getOrThrow()

            val thumbnailPath = httpDataSource.downloadImage(dto.thumbnailDownloadUrl).getOrNull()
                ?.let { thumbnail ->
                    fileManager.saveImageWithCompression(thumbnail, videoId).getOrNull()
                }

            AudioMediaData(
                videoId = videoId,
                audioFilePath = audioDownloadPath,
                imageFilePath = thumbnailPath,
                title = dto.title,
                description = "",
            )
        }

    override suspend fun getMediaDataFromMetaData(videoId: String): Result<AudioMediaData> =
        runCatching {
            val entity = youtubeMediaDataSource.getYoutubeData(videoId).getOrNull()

            val audioFilePath =
                fileManager.getFullPathWithExt(entity!!.audioFileNameWithExt).getOrThrow()
            val imageFilePath = entity.imageFileNameWithExt?.let {
                fileManager.getFullPathWithExt(it).getOrNull()
            }

            AudioMediaData(
                videoId = entity.id,
                audioFilePath = audioFilePath,
                imageFilePath = imageFilePath,
                title = entity.title,
                description = entity.description,
            )
        }

    override suspend fun saveMetaData(mediaData: AudioMediaData): Result<Unit> = runCatching {
        val audioFileNameWithExt =
            fileManager.getFileNameWithExt(mediaData.audioFilePath).getOrThrow()
        val imageFileNameWithExt = mediaData.imageFilePath?.let {
            fileManager.getFileNameWithExt(it).getOrNull()
        }

        youtubeMediaDataSource.saveYoutubeData(
            AudioMediaEntity(
                id = mediaData.videoId,
                title = mediaData.title,
                description = mediaData.description,
                audioFileNameWithExt = audioFileNameWithExt,
                imageFileNameWithExt = imageFileNameWithExt,
            )
        )
    }

    override suspend fun deleteMediaData(videoId: String): Result<Unit> = runCatching {
        fileManager.deleteFile(videoId, FileManager.Ext.MP3)
        fileManager.deleteFile(videoId, FileManager.Ext.WEBP)
        fileManager.deleteFile(videoId, FileManager.Ext.JPG)
    }

    override fun clearCacheData(): Result<Unit> {
        return fileManager.clearCache()
    }
}