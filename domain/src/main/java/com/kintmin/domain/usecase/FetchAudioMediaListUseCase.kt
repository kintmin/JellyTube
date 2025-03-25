package com.kintmin.domain.usecase

import com.kintmin.domain.model.AudioMediaData
import com.kintmin.domain.repository.AudioMediaRepository
import javax.inject.Inject

class FetchAudioMediaListUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
){
    suspend operator fun invoke(): Result<List<AudioMediaData>> {
        return audioMediaRepository.getListDataAll()
    }
}