package com.kintmin.ytmusicbox.data.local

import android.content.Context
import android.os.Environment
import com.kintmin.ytmusicbox.data.local.dao.YoutubeMediaDao
import com.kintmin.ytmusicbox.data.local.entity.YoutubeMediaEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class LocalFileDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youtubeMediaDao: YoutubeMediaDao,
) {

    suspend fun getYoutubeData(videoId: String) = runCatching {
        withContext(Dispatchers.IO) {
            youtubeMediaDao.getMediaById(videoId)
        }
    }

    suspend fun saveYoutubeData(
        videoId: String,
        title: String?,
        description: String?,
        rawData: ByteArray,
    ) = runCatching {
        withContext(Dispatchers.IO) {
            val fileName = "$videoId.m3u8"
            val outputFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
            outputFile.writeBytes(rawData)

            youtubeMediaDao.insertMedia(
                YoutubeMediaEntity(
                    videoId,
                    title ?: "알 수 없음",
                    description ?: "",
                    outputFile.path
                )
            )
        }
    }

    suspend fun deleteYoutubeData(videoId: String) {
        withContext(Dispatchers.IO) {
            youtubeMediaDao.getMediaById(videoId)?.filePath?.let { path ->
                val file = File(path)
                if (file.exists() && !file.delete()) {
                    println("파일 삭제 실패: ${file.path}")
                }
            }
            youtubeMediaDao.deleteMediaById(videoId)
        }
    }
}