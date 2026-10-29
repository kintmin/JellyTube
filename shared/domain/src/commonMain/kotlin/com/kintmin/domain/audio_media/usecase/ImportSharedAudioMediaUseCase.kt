package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import kotlinx.coroutines.flow.first

class ImportSharedAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
) {
    /**
     * @param playlistIdOverride 이번 가져오기에만 적용할 대상 재생목록. null이면 "다운로드 시 재생목록"
     * 전역 설정값을 쓴다. 가져오기 직전에 재생목록을 고르는 화면에서만 넘긴다(전역 설정은 바뀌지 않는다).
     */
    suspend operator fun invoke(
        contentUriString: String,
        playlistIdOverride: Int? = null,
    ): Result<AudioMedia> = runCatching {
        val shouldInsertAtTop = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistId = playlistIdOverride ?: fetchPlaylistIdOnDownloadFlowUseCase().first()

        // 대상 해석과 시스템 플레이리스트 보장은 데이터 계층이 담당한다.
        audioMediaRepository.importSharedAudio(
            contentUriString = contentUriString,
            playlistIdOnDownload = playlistId,
            shouldInsertAtTopOnDownload = shouldInsertAtTop,
        ).getOrThrow().audioMedia
    }
}
