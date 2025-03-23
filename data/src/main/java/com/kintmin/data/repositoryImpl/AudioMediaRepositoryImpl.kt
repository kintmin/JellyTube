package com.kintmin.data.repositoryImpl

import com.kintmin.data.local.datasource.FileManager
import com.kintmin.data.local.datasource.LocalAudioDataSource
import com.kintmin.domain.model.AudioMediaData
import com.kintmin.domain.repository.AudioMediaRepository
import javax.inject.Inject

class AudioMediaRepositoryImpl @Inject constructor(
    private val localAudioDataSource: LocalAudioDataSource,
    private val fileManager: FileManager,
) : AudioMediaRepository {
    override suspend fun getAudioMediaList(): Result<List<AudioMediaData>> {
        return localAudioDataSource.getEntityList().mapCatching { entityList ->
            entityList.mapNotNull { entity ->
                val audioFileFullPath = fileManager.getFullPathWithExt(entity.audioFileNameWithExt).getOrNull()
                val imageFileFullPath = entity.imageFileNameWithExt?.let {
                    fileManager.getFullPathWithExt(it).getOrNull()
                }

                audioFileFullPath?.let {
                    AudioMediaData(
                        videoId = entity.id,
                        audioFileFullPath = it,
                        imageFileFullPath = imageFileFullPath,
                        title = entity.title,
                        description = entity.description,
                    )
                }
            }
        }
    }
}