package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class FetchAudioMediaDetailFlowUseCase constructor(
    private val playlistTrackRepository: AudioTrackRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(audioMediaId: Int): Flow<List<PlaylistTrackAggregate>> {
        // ?°ê²°???Œë ˆ?´ë¦¬?¤íŠ¸ë¥??„ë? ê°€?¸ì˜¨??
        return playlistTrackRepository.getPlaylistIdListFlow(audioMediaId)
            .flowOn(Dispatchers.IO)
            // ?°ê²°???Œë ˆ?´ë¦¬?¤íŠ¸ê°€ ë³€ê²???flow ?¤ì‹œ ?ì„±
            .flatMapLatest { playlistIds ->
                if (playlistIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    // ?Œë ˆ?´ë¦¬?¤íŠ¸ flow ê°€ ?„ë? ?ì–´??1ë²?collect ?˜ì–´???˜ê³ ,
                    // ?˜ë‚˜?¼ë„ ë³€ê²½ë  ???¤ì‹œ collect ?´ì•¼?˜ê¸°??combine ?¬ìš©
                    combine(
                        playlistIds.map { playlistId ->
                            playlistTrackRepository
                                .getPlaylistTrackAggregateFlow(playlistId, audioMediaId)
                                .flowOn(Dispatchers.IO)
                        }
                    ) { it.toList() }
                }
            }
    }
}