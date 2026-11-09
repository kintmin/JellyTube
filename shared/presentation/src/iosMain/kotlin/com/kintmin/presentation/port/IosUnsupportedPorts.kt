package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.presentation.port.model.MediaControlData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * iOS 포트 미구현 스텁 모음.
 *
 * **이 파일이 없으면 iOS 에서 화면을 띄울 수 없다.** `presentationModule` 의 ViewModel 들이
 * 이 포트들을 생성자로 받으므로, 바인딩이 비어 있으면 해당 화면에 들어가는 순간 Koin 이
 * 터진다. `shared:data` 의 `AudioEditorImpl` 이 같은 이유로 먼저 둔 처방이다.
 *
 * **실패를 조용히 삼키지 않는다.** `Result` 를 돌려주는 자리는 실패를, 아닌 자리는 기본값을
 * 돌려준다. 호출자가 이미 그 둘을 다루게 되어 있다.
 *
 * 각 스텁은 실구현이 들어오면 **파일에서 사라진다.** 영구 잔류가 확정된 것(기능 자체를 iOS 에서
 * 제공하지 않기로 한 것)은 그렇게 적어 뒀다.
 */
private fun unsupported(feature: String) =
    UnsupportedOperationException("iOS 에서는 $feature 을 지원하지 않습니다.")

/** 교체 예정 — AVAudioEngine 기반 재생으로 채운다. */
internal class IosMediaControllerManagerStub : MediaControllerManager {

    override fun initialize() = Unit

    override val isPlaying: Boolean = false
    override fun pause() = Unit
    override fun resume() = Unit
    override fun playPrevious() = Unit
    override fun playNext() = Unit

    override val playingMediaItem: MediaControlData? = null
    override val currentPlaylistId: Int? = null
    override val currentPosition: Long? = null
    override val playbackDuration: Long? = null
    override val playbackSpeed: Float = 1f
    override val playbackPitchSemitone: Int = 0
    override val repeatRangeState: RepeatRangeState = RepeatRangeState()

    override fun seek(duration: Duration) = Unit
    override fun updateRepeatRange(): Result<Unit> = Result.failure(unsupported("재생"))
    override fun clearRepeatRange() = Unit
    override fun repeatRangeIfNeeded() = Unit

    override fun playFromPlaylist(
        playlistId: Int,
        startMediaId: Int?,
        mediaControlDataList: List<MediaControlData>,
    ): Result<Unit> = Result.failure(unsupported("재생"))

    override fun tryDeleteMediaItem(playlistId: Int, mediaId: Int): Result<Unit> =
        Result.failure(unsupported("재생"))

    override fun tryAddLastMediaItem(playlistId: Int, mediaItem: MediaControlData): Result<Unit> =
        Result.failure(unsupported("재생"))

    override fun tryAddFirstMediaItem(playlistId: Int, mediaItem: MediaControlData): Result<Unit> =
        Result.failure(unsupported("재생"))

    override fun tryReplaceMediaItem(mediaId: Int, mediaItem: MediaControlData): Result<Unit> =
        Result.failure(unsupported("재생"))

    override fun setShuffleMode(isShuffle: Boolean) = Unit
    override fun setRepeatMode(isRepeat: Boolean) = Unit
    override fun setPlaybackSpeed(speed: Float) = Unit
    override fun setPlaybackPitchSemitone(semitone: Int) = Unit
}

/** **영구 잔류** — 음원 편집을 iOS 에서 제공하지 않는다. 편집 화면 진입점도 숨긴다. */
internal class IosAudioPreviewPlayerStub : AudioPreviewPlayer {

    override val isPlaying: Boolean = false
    override val currentPositionMs: Long? = null

    override fun play(audioFileFullPath: String, parts: List<AudioEditPart>, startAtMs: Long) = Unit
    override fun pause() = Unit
    override fun release() = Unit
}

/**
 * **영구 잔류** — PC 파일 수신을 iOS 에서 제공하지 않는다.
 * `shared:file-share` 가 JVM 전용 모듈이라 상수조차 공유되지 않는다.
 */
internal class IosFileShareServerControllerStub : FileShareServerController {

    override val isRunning: StateFlow<Boolean> = MutableStateFlow(false)

    override fun start() = Unit
    override fun stop() = Unit
}

/** 교체 예정 — yt-dlp 추출 + background URLSession 전송으로 채운다. */
internal class IosExecuteYoutubeDownloadStub : ExecuteYoutubeDownload {

    override fun invoke(url: String) = Unit
}

/** 교체 예정 — 앱 수명 코루틴 스코프로 채운다. */
internal class IosExecuteAnomalyDataCheckStub : ExecuteAnomalyDataCheck {

    /** false 는 "이미 실행 중" 이 아니라 "넘기지 못했다" 는 뜻으로 읽힌다 — 호출자가 그렇게 다룬다. */
    override suspend fun invoke(): Boolean = false
}

/** 교체 예정 — 앱 수명 코루틴 스코프로 채운다. */
internal class IosExecuteLyricsVariantCreationStub : ExecuteLyricsVariantCreation {

    override fun invoke(
        audioMediaId: Int,
        lyricFileFullPath: String,
        variant: LyricsVariant,
        sourceLanguage: String,
    ) = Unit
}

/** **영구 잔류** — 음원 편집을 iOS 에서 제공하지 않는다. */
internal class IosExecuteAudioEditSaveStub : ExecuteAudioEditSave {

    override fun overwrite(audioMediaId: Int, timeline: AudioEditTimeline): Result<Unit> =
        Result.failure(unsupported("음원 편집"))

    override fun saveAsNewMedia(
        audioMediaId: Int,
        timeline: AudioEditTimeline,
        name: String,
    ): Result<Unit> = Result.failure(unsupported("음원 편집"))
}
