package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class DeleteAudioMediaListUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase,
    private val log: Log,
) {
    suspend operator fun invoke(idList: List<Int>, sourceList: List<String>): Result<Unit> = runCatching {
        supervisorScope {
            val mutex = Mutex()
            val targetPlaylistIdSet = mutableSetOf<Int>()

            // 삭제할 미디어의 모든 트랙을 삭제하고,
            // 삭제에 관여된 모든 플레이리스트 id를 가져온다.
            idList.mapIndexed { index, id ->
                launch {
                    val targetPlaylistIdList = audioMediaRepository.deleteAudioMedia(id).onSuccess {
                        log.sendFirebaseEvent(FirebaseEvent.DeleteAudioMedia(sourceList[index]))
                    }.getOrThrow()

                    mutex.withLock {
                        targetPlaylistIdSet.addAll(targetPlaylistIdList)
                    }
                }
            }.joinAll()

            // 변경된 플레이리스트는 전부 업데이트한다.
            targetPlaylistIdSet.map { playlistId ->
                launch { updateOnPlaylistChangeUseCase(playlistId) }
            }.joinAll()
        }
    }
}