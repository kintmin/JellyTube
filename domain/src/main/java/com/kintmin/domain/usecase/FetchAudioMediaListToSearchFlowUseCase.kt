package com.kintmin.domain.usecase

import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class FetchAudioMediaListToSearchFlowUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
){
    operator fun invoke(playlistId: Int): Flow<List<AudioMedia>> {
        return combine(
            audioMediaRepository.getAudioMediaListFlow(Playlist.TOTAL),
            audioMediaRepository.getAudioMediaListFlow(playlistId),
        ) { totalList, currentList ->
            totalList.filter { total -> currentList.firstOrNull { it.id == total.id } == null }
        }.distinctUntilChanged()
    }
}