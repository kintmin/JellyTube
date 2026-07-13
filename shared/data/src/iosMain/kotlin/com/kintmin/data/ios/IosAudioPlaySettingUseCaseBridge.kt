package com.kintmin.data.ios

import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackPitchSemitoneUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackSpeedUseCase
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class IosFetchPlaybackSpeedFlowUseCaseBridge : KoinComponent {

    operator fun invoke(): Flow<Float> =
        get<FetchPlaybackSpeedFlowUseCase>()()
}

fun createIosFetchPlaybackSpeedFlowUseCaseBridge(): IosFetchPlaybackSpeedFlowUseCaseBridge =
    IosFetchPlaybackSpeedFlowUseCaseBridge()

class IosUpdatePlaybackSpeedUseCaseBridge : KoinComponent {

    suspend operator fun invoke(speed: Float): Result<Unit> =
        get<UpdatePlaybackSpeedUseCase>()(speed)
}

fun createIosUpdatePlaybackSpeedUseCaseBridge(): IosUpdatePlaybackSpeedUseCaseBridge =
    IosUpdatePlaybackSpeedUseCaseBridge()

class IosFetchPlaybackPitchSemitoneFlowUseCaseBridge : KoinComponent {

    operator fun invoke(): Flow<Int> =
        get<FetchPlaybackPitchSemitoneFlowUseCase>()()
}

fun createIosFetchPlaybackPitchSemitoneFlowUseCaseBridge(): IosFetchPlaybackPitchSemitoneFlowUseCaseBridge =
    IosFetchPlaybackPitchSemitoneFlowUseCaseBridge()

class IosUpdatePlaybackPitchSemitoneUseCaseBridge : KoinComponent {

    suspend operator fun invoke(semitone: Int): Result<Unit> =
        get<UpdatePlaybackPitchSemitoneUseCase>()(semitone)
}

fun createIosUpdatePlaybackPitchSemitoneUseCaseBridge(): IosUpdatePlaybackPitchSemitoneUseCaseBridge =
    IosUpdatePlaybackPitchSemitoneUseCaseBridge()
