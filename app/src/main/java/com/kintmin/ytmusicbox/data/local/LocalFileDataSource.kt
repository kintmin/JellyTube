package com.kintmin.ytmusicbox.data.local

import com.kintmin.ytmusicbox.data.local.dao.YoutubeMediaDao
import com.kintmin.ytmusicbox.data.local.entity.YoutubeMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class LocalFileDataSource @Inject constructor(
    private val youtubeMediaDao: YoutubeMediaDao,
) {

    suspend fun getYoutubeData(videoId: String) = runCatching {
        withContext(Dispatchers.IO) {
            youtubeMediaDao.getMediaById(videoId)
        }
    }

    suspend fun saveYoutubeData(
        videoId: String,
        title: String,
        audioFilePath: String,
        imageFilePath: String,
    ) = runCatching {
        withContext(Dispatchers.IO) {
            youtubeMediaDao.insertMedia(
                YoutubeMediaEntity(
                    id = videoId,
                    title = title,
                    description = "",
                    audioFilePath = audioFilePath,
                    imageFilePath = imageFilePath,
                )
            )
        }
    }

    suspend fun deleteYoutubeData(videoId: String) {
        withContext(Dispatchers.IO) {
            val mediaEntity = youtubeMediaDao.getMediaById(videoId)
            listOf(mediaEntity?.imageFilePath, mediaEntity?.audioFilePath).forEach { path ->
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        runCatching { file.delete() }
                    }
                }
            }

            youtubeMediaDao.deleteMediaById(videoId)
        }
    }
}