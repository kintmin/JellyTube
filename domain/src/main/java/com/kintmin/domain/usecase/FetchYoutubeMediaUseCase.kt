package com.kintmin.domain.usecase

import com.kintmin.domain.model.AudioMediaData
import com.kintmin.domain.repository.YoutubeMediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FetchYoutubeMediaUseCase @Inject constructor(
    private val youtubeMediaRepository: YoutubeMediaRepository,
    private val extractYoutubeVideoIdUseCase: ExtractYoutubeVideoIdUseCase,
) {
    fun getDataStream(youtubeUrl: String): Flow<AudioMediaData> = flow {
        val videoId = extractYoutubeVideoIdUseCase(youtubeUrl)
        val mediaData = youtubeMediaRepository.getMediaDataFromMetaData(videoId).getOrNull()
        if (mediaData != null) {
            emit(mediaData)
            return@flow
        }

        youtubeMediaRepository.getMediaData(youtubeUrl, videoId)
            .onSuccess { media ->
                youtubeMediaRepository.clearCacheData()
                youtubeMediaRepository.saveMetaData(media)
                    .onFailure {
                        youtubeMediaRepository.deleteMediaData(videoId)
                        throw it
                    }
                emit(media)
            }.onFailure {
                throw it
            }
    }

    suspend fun getData(youtubeUrl: String): Result<AudioMediaData> = runCatching {
        getDataStream(youtubeUrl).first()
    }
}