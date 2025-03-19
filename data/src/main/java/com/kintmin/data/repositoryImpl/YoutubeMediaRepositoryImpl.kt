package com.kintmin.data.repositoryImpl

import com.kintmin.data.local.datasource.FileManager
import com.kintmin.data.local.datasource.YoutubeMediaDataSource
import com.kintmin.data.local.datasource.PythonExecutor
import com.kintmin.data.local.entity.AudioMediaEntity
import com.kintmin.domain.repository.YoutubeMediaRepository
import com.kintmin.domain.model.AudioMediaData
import javax.inject.Inject

class YoutubeMediaRepositoryImpl @Inject constructor(
    private val youtubeMediaDataSource: YoutubeMediaDataSource,
    private val fileManager: FileManager,
    private val pythonExecutor: PythonExecutor
) : YoutubeMediaRepository {
    override fun isExistFile(videoId: String): Result<Boolean> {
        return fileManager.isExistFile(videoId, FileManager.FileType.Audio)
    }

    override suspend fun isExistData(videoId: String) = runCatching {
        youtubeMediaDataSource.getYoutubeData(videoId).getOrThrow() != null
    }

    override suspend fun deleteData(videoId: String): Result<Unit> {
        return youtubeMediaDataSource.deleteYoutubeData(videoId)
    }

    override suspend fun saveData(mediaData: AudioMediaData): Result<Unit> = runCatching {
        val imageFileName = mediaData.imageFilePath?.let {
            fileManager.getFileNameFromPath(it) ?: throw Exception("이미지 파일 확장자를 찾을 수 없습니다.")
        }
        youtubeMediaDataSource.saveYoutubeData(
            AudioMediaEntity(
                id = mediaData.videoId,
                title = mediaData.title,
                description = mediaData.description,
                imageFileName = imageFileName,
            )
        )
    }

    override suspend fun getCachedMediaData(videoId: String): Result<AudioMediaData> =
        runCatching {
            val entity = youtubeMediaDataSource.getYoutubeData(videoId).getOrThrow()!!
            val audioFilePath = fileManager.getFullPath(entity.id, FileManager.Ext.mp3).getOrThrow()
            val imageFilePath = entity.imageFileName?.let {
                fileManager.getFullPath(it, FileManager.Ext.webp).getOrThrow()
            }

            AudioMediaData(
                videoId = entity.id,
                audioFilePath = audioFilePath,
                imageFilePath = imageFilePath,
                title = entity.title,
                description = entity.description,
            )
        }


    override suspend fun getMediaData(videoId: String): Result<AudioMediaData> = runCatching {
        val audioDownloadPath = fileManager.getFullPath(videoId, FileManager.Ext.mp3).getOrThrow()
        val dto = pythonExecutor.downloadYoutubeMedia(videoId, audioDownloadPath).getOrThrow()

        // 유튜브 형식 Data 가 따로 있어야 할 듯. path 가 아니라 썸네일 다운 주소임.
        AudioMediaData(
            videoId = videoId,
            audioFilePath = audioDownloadPath,
            imageFilePath = dto.thumbnailPath,
            title = dto.title,
            description = "",
        )
    }
}