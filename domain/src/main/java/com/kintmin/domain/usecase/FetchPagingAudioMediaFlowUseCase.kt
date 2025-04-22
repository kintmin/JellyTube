package com.kintmin.domain.usecase

import androidx.paging.PagingData
import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchPagingAudioMediaFlowUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
){
    operator fun invoke(): Flow<PagingData<AudioMedia>> {
        return audioMediaRepository.getPagingAudioMediaFlow()
    }
}