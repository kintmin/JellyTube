package com.kintmin.ios.media

import com.kintmin.presentation.port.MediaControllerManager
import com.kintmin.presentation.port.RepeatRangeState
import com.kintmin.presentation.port.model.MediaControlData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * iOS 재생 포트.
 *
 * 재생·큐·셔플·반복은 [IosMediaControllerBridge] 를 거쳐 Swift 엔진이 하고, **A-B 구간반복과
 * 현재 플레이리스트 추적만 여기서 한다.** Android 구현(`MediaControllerManagerImpl`)도
 * 그 둘만 앱 코드로 들고 나머지는 ExoPlayer 에 맡긴다 — 같은 가름이다.
 *
 * 속도·피치도 Android 와 같게 **이쪽이 기억**한다. 플레이어에게 물으면 플랫폼마다 반올림이
 * 달라 화면이 흔들린다.
 */
internal class IosMediaControllerManager(
    private val bridge: IosMediaControllerBridge,
) : MediaControllerManager {

    private var currentPlaylistIdState: Int? = null
    private var playbackSpeedState = 1.0f
    private var playbackPitchSemitoneState = 0

    private var repeatRangeMediaId: String? = null
    private var repeatRangeStart: Duration? = null
    private var repeatRangeEnd: Duration? = null

    override fun initialize() = bridge.initialize()

    override val isPlaying: Boolean
        get() = bridge.isPlaying()

    override fun pause() = bridge.pause()

    override fun resume() = bridge.resume()

    override fun playPrevious() = bridge.playPrevious()

    override fun playNext() = bridge.playNext()

    override val playingMediaItem: MediaControlData?
        get() = bridge.playingItem()?.toMediaControlData()

    override val currentPlaylistId: Int?
        get() = currentPlaylistIdState

    override val currentPosition: Long?
        get() = bridge.currentPositionMillis()

    override val playbackDuration: Long?
        get() = bridge.durationMillis()

    override val playbackSpeed: Float
        get() = playbackSpeedState

    override val playbackPitchSemitone: Int
        get() = playbackPitchSemitoneState

    override val repeatRangeState: RepeatRangeState
        get() {
            clearRepeatRangeIfMediaChanged()
            return RepeatRangeState(
                mediaId = repeatRangeMediaId,
                startDuration = repeatRangeStart,
                endDuration = repeatRangeEnd,
            )
        }

    override fun seek(duration: Duration) = bridge.seek(duration.inWholeMilliseconds)

    /** 단계와 조건은 `IosMediaControllerManagerTest` 가 고정한다. Android 구현과 같아야 한다. */
    override fun updateRepeatRange(): Result<Unit> = runCatching {
        val mediaId = bridge.playingItem()?.mediaId ?: return@runCatching
        clearRepeatRangeIfMediaChanged(mediaId)

        val currentDuration = bridge.currentPositionMillis().milliseconds
        when {
            repeatRangeStart == null -> {
                repeatRangeMediaId = mediaId
                repeatRangeStart = currentDuration
            }

            repeatRangeEnd == null -> {
                val start = repeatRangeStart ?: return@runCatching
                check(currentDuration > start)
                repeatRangeEnd = currentDuration
            }

            else -> clearRepeatRange()
        }
    }

    override fun clearRepeatRange() {
        repeatRangeMediaId = null
        repeatRangeStart = null
        repeatRangeEnd = null
    }

    override fun repeatRangeIfNeeded() {
        val mediaId = bridge.playingItem()?.mediaId ?: return
        clearRepeatRangeIfMediaChanged(mediaId)

        val start = repeatRangeStart ?: return
        val end = repeatRangeEnd ?: return
        if (bridge.currentPositionMillis().milliseconds >= end) {
            bridge.seek(start.inWholeMilliseconds)
        }
    }

    override fun playFromPlaylist(
        playlistId: Int,
        startMediaId: Int?,
        mediaControlDataList: List<MediaControlData>,
    ): Result<Unit> = runCatching {
        currentPlaylistIdState = playlistId
        bridge.play(
            playlistId = playlistId,
            startMediaId = startMediaId,
            items = mediaControlDataList.map { it.toIosMediaItem() },
        )
    }

    override fun tryDeleteMediaItem(playlistId: Int, mediaId: Int): Result<Unit> = runCatching {
        if (currentPlaylistIdState != playlistId) return@runCatching
        bridge.removeItem(mediaId.toString())
    }

    override fun tryAddLastMediaItem(
        playlistId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit> = runCatching {
        if (currentPlaylistIdState != playlistId) return@runCatching
        bridge.addItem(mediaItem.toIosMediaItem(), atStart = false)
    }

    override fun tryAddFirstMediaItem(
        playlistId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit> = runCatching {
        if (currentPlaylistIdState != playlistId) return@runCatching
        bridge.addItem(mediaItem.toIosMediaItem(), atStart = true)
    }

    override fun tryReplaceMediaItem(
        mediaId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit> = runCatching {
        bridge.replaceItem(mediaId.toString(), mediaItem.toIosMediaItem())

        // 길이가 바뀌면 옛 길이 기준으로 잡아둔 구간반복은 더 이상 유효하지 않다.
        if (repeatRangeMediaId == mediaId.toString()) {
            clearRepeatRange()
        }
    }

    override fun setShuffleMode(isShuffle: Boolean) = bridge.setShuffleMode(isShuffle)

    override fun setRepeatMode(isRepeat: Boolean) = bridge.setRepeatMode(isRepeat)

    override fun setPlaybackSpeed(speed: Float) {
        playbackSpeedState = speed
        bridge.setPlaybackSpeed(speed)
    }

    override fun setPlaybackPitchSemitone(semitone: Int) {
        playbackPitchSemitoneState = semitone
        bridge.setPlaybackPitchSemitone(semitone)
    }

    private fun clearRepeatRangeIfMediaChanged(mediaId: String? = bridge.playingItem()?.mediaId) {
        if (repeatRangeMediaId != null && repeatRangeMediaId != mediaId) {
            clearRepeatRange()
        }
    }
}

private fun MediaControlData.toIosMediaItem() = IosMediaItem(
    mediaId = mediaId,
    fileFullPath = mediaFileUri,
    title = mediaTitle,
    description = mediaDescription,
    artist = mediaArtist,
    durationMillis = mediaDurationMs,
    artworkFileFullPath = mediaArtworkFileUri,
)

private fun IosMediaItem.toMediaControlData() = MediaControlData(
    mediaId = mediaId,
    mediaFileUri = fileFullPath,
    mediaTitle = title,
    mediaDescription = description,
    mediaArtist = artist,
    mediaDurationMs = durationMillis,
    mediaArtworkFileUri = artworkFileFullPath,
)
