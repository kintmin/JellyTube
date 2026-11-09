package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.DownloadLyricsForAudioMediaUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.presentation.port.model.MediaControlData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit

/**
 * 유튜브 음원 다운로드를 건다.
 *
 * **Android 의 WorkManager 에 해당하는 것이 iOS 에는 없다.** 대신 전송 자체가
 * background `URLSession` 으로 OS 데몬에 넘어가 있어(`YoutubeDownloader`) 앱이 내려가도
 * 이어진다. 여기서 도는 코루틴은 **추출과 등록**만 맡는다.
 *
 * 앱이 없는 사이에 전송이 끝났다면 등록이 밀리는데, 앱이 다시 뜰 때 같은 주소로 한 번 더
 * 태우면 전송 없이 등록까지 이어진다(`takeFinishedDownloadUrls`).
 *
 * Android 워커와 순서를 맞췄다 — 음원 등록 → 알림 → 재생목록 반영, 가사는 병렬로 조용히.
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
            downloadAudioMediaUseCase(url)
                .onSuccess { result ->
                    // 가사는 실패해도 음원 등록을 되돌리지 않는다. 등록이 끝난 뒤라 고아가 생기지 않는다.
                    scope.launch {
                        downloadLyricsForAudioMediaUseCase(
                            audioMediaId = result.audioMedia.id,
                            title = result.audioMedia.name,
                            targetDurationSeconds = result.audioMedia.audioDuration
                                ?.toDouble(DurationUnit.SECONDS),
                        )
                    }

                    IosLocalNotification.show(
                        title = "다운로드 완료",
                        body = "${result.audioMedia.artist} - ${result.audioMedia.name}",
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
                    IosLocalNotification.show(
                        title = "다운로드 실패",
                        body = throwable.message ?: "다운에 실패했습니다. 로그를 확인해주세요.",
                    )
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
