package com.kintmin.ios.port

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.DownloadLyricsForAudioMediaUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.presentation.platform.continueInBackgroundWhile
import com.kintmin.presentation.port.ExecuteYoutubeDownload
import com.kintmin.presentation.port.MediaControllerManager
import com.kintmin.presentation.port.WorkResultNotice
import com.kintmin.presentation.port.model.MediaControlData
import com.kintmin.presentation.port.model.RunningWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit

/**
 * 유튜브 음원 다운로드를 건다.
 *
 * 음원 바이트는 `URLSession` 이 받지만 추출·썸네일 받기·DB 기록은 앱이 해야 한다. 그래서
 * 작업 전체를 실행 유예로 감싼다 — 감싸지 않으면 홈으로 내리는 순간 멈춰서, 음원만 들어오고
 * 썸네일이 빠지거나 아예 시작도 못 한다.
 */
internal class IosExecuteYoutubeDownload(
    private val downloadAudioMediaUseCase: DownloadAudioMediaUseCase,
    private val downloadLyricsForAudioMediaUseCase: DownloadLyricsForAudioMediaUseCase,
    private val mediaControllerManager: MediaControllerManager,
    private val appLog: AppLog,
) : ExecuteYoutubeDownload {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun invoke(url: String) {
        scope.launch {
            IosRunningWorkTracker.track(RunningWork.YOUTUBE_DOWNLOAD) {
                continueInBackgroundWhile { downloadAudioMediaUseCase(url) }
            }
                .onSuccess { result ->
                    // 가사는 실패해도 음원 등록을 되돌리지 않는다. 등록이 끝난 뒤라 고아가 생기지 않는다.
                    scope.launch {
                        continueInBackgroundWhile {
                            downloadLyricsForAudioMediaUseCase(
                                audioMediaId = result.audioMedia.id,
                                title = result.audioMedia.name,
                                targetDurationSeconds = result.audioMedia.audioDuration
                                    ?.toDouble(DurationUnit.SECONDS),
                            )
                        }
                    }

                    IosLocalNotification.show(
                        WorkResultNotice.Download.Success(
                            artist = result.audioMedia.artist,
                            name = result.audioMedia.name,
                            playlistId = result.playlistIdOnDownload,
                            audioMediaId = result.audioMedia.id,
                        ),
                    )

                    val mediaControlData = result.audioMedia.toMediaControlData()
                    addToPlayingQueue(result.totalPlaylistId, mediaControlData, result.shouldInsertAtTopOnDownload)
                    if (result.playlistIdOnDownload != result.totalPlaylistId) {
                        addToPlayingQueue(
                            result.playlistIdOnDownload,
                            mediaControlData,
                            result.shouldInsertAtTopOnDownload,
                        )
                    }
                }
                .onFailure { throwable ->
                    appLog.sendDebugLog(
                        DebugLog("IosExecuteYoutubeDownload", throwable.message.orEmpty()),
                    )
                    IosLocalNotification.show(WorkResultNotice.Download.Failure)
                }
        }
    }

    /** 지금 그 플레이리스트를 듣고 있을 때만 큐에 반영된다. 아니면 포트가 조용히 넘긴다. */
    private fun addToPlayingQueue(
        playlistId: Int,
        mediaControlData: MediaControlData,
        shouldInsertAtTop: Boolean,
    ) {
        if (shouldInsertAtTop) {
            mediaControllerManager.tryAddFirstMediaItem(playlistId, mediaControlData)
        } else {
            mediaControllerManager.tryAddLastMediaItem(playlistId, mediaControlData)
        }
    }
}


private fun AudioMedia.toMediaControlData() = MediaControlData(
    mediaId = id.toString(),
    mediaFileUri = playbackFileFullPath,
    mediaTitle = name,
    mediaDescription = description,
    mediaArtist = artist,
    mediaDurationMs = playbackDuration?.inWholeMilliseconds ?: 0L,
    mediaArtworkFileUri = imageFileFullPath,
)
