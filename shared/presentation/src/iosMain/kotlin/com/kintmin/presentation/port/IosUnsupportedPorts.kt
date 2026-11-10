package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
 * 여기 남은 셋은 **영구 잔류**다 — 기능 자체를 iOS 에서 제공하지 않기로 한 것들이다.
 * 교체 예정이던 것들은 실구현이 들어오면서 이 파일을 떠났다.
 */
private fun unsupported(feature: String) =
    UnsupportedOperationException("iOS 에서는 $feature 을 지원하지 않습니다.")

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
