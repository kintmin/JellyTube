package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS 가 제공하지 않는 포트의 스텁.
 *
 * **바인딩을 비워 두면 안 된다.** `presentationModule` 의 ViewModel 들이 이 포트들을
 * 생성자로 받으므로, 비어 있으면 해당 화면에 들어가는 순간 Koin 이 터진다.
 *
 * 실패를 조용히 삼키지 않는다 — `Result` 자리는 실패를, 아닌 자리는 기본값을 돌려준다.
 */
private fun unsupported(feature: String) =
    UnsupportedOperationException("iOS 에서는 $feature 을 지원하지 않습니다.")

/** 음원 편집 미제공. 진입점은 `isAudioEditSupported` 가 숨긴다. */
internal class IosAudioPreviewPlayerStub : AudioPreviewPlayer {

    override val isPlaying: Boolean = false
    override val currentPositionMs: Long? = null

    override fun play(audioFileFullPath: String, parts: List<AudioEditPart>, startAtMs: Long) = Unit
    override fun pause() = Unit
    override fun release() = Unit
}

/** PC 파일 수신 미제공. `shared:file-share` 가 JVM 전용 모듈이라 상수조차 공유되지 않는다. */
internal class IosFileShareServerControllerStub : FileShareServerController {

    override val isRunning: StateFlow<Boolean> = MutableStateFlow(false)

    override fun start() = Unit
    override fun stop() = Unit
}

/** 음원 편집 미제공. */
internal class IosExecuteAudioEditSaveStub : ExecuteAudioEditSave {

    override fun overwrite(audioMediaId: Int, timeline: AudioEditTimeline): Result<Unit> =
        Result.failure(unsupported("음원 편집"))

    override fun saveAsNewMedia(
        audioMediaId: Int,
        timeline: AudioEditTimeline,
        name: String,
    ): Result<Unit> = Result.failure(unsupported("음원 편집"))
}
