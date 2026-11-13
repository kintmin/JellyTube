package com.kintmin.ios.port

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.ExecuteAudioEditSave
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS 가 제공하지 않는 포트의 스텁. ViewModel 이 생성자로 받으므로 **바인딩을 비워 두면**
 * 해당 화면에 들어가는 순간 Koin 이 터진다.
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

/** 데이터 보내기 미제공. iOS 는 받는 쪽만 한다 — 보내는 서버가 Android 에만 있다. */
internal class IosDataMigrationSendControllerStub : DataMigrationSendController {

    override val status: StateFlow<DataMigrationSendStatus> = MutableStateFlow(DataMigrationSendStatus.Idle)

    override fun start() = Unit
    override fun stop() = Unit
}

internal class IosExecuteAudioEditSaveStub : ExecuteAudioEditSave {

    override fun overwrite(audioMediaId: Int, timeline: AudioEditTimeline): Result<Unit> =
        Result.failure(unsupported("음원 편집"))

    override fun saveAsNewMedia(
        audioMediaId: Int,
        timeline: AudioEditTimeline,
        name: String,
    ): Result<Unit> = Result.failure(unsupported("음원 편집"))
}
