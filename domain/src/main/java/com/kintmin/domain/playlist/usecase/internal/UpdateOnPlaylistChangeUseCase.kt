package com.kintmin.domain.playlist.usecase.internal

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

/**
 * AudioTrack 데이터 Update, Delete 시 사용 필수
 */
class UpdateOnPlaylistChangeUseCase @Inject constructor(
    private val updatePlaylistCountAndPlayTimeUseCase: UpdatePlaylistCountAndPlayTimeUseCase,
    private val updatePlaylistImageUseCase: UpdatePlaylistImageUseCase,
) {

    suspend operator fun invoke(playlistId: Int): Result<Unit> {
        return runCatching {
            supervisorScope {
                listOf(
                    launch { updatePlaylistCountAndPlayTimeUseCase(playlistId) },
                    launch { updatePlaylistImageUseCase(playlistId) },
                ).joinAll()
            }
        }
    }
}