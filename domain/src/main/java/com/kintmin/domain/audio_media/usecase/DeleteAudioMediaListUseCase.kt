package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class DeleteAudioMediaListUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase,
) {
    suspend operator fun invoke(idList: List<Int>): Result<Unit> = runCatching {
        supervisorScope {
            val mutex = Mutex()
            val targetPlaylistIdSet = mutableSetOf<Int>()

            // 삭제할 미디어의 모든 트랙을 삭제하고,
            // 삭제에 관여된 모든 플레이리스트 id를 가져온다.
            idList.map { id ->
                launch {
                    val targetPlaylistIdList = audioMediaRepository.deleteAudioMedia(id).getOrThrow()
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