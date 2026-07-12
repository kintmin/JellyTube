package com.kintmin.data.ios

import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class IosDownloadAudioMediaUseCaseBridge : KoinComponent {

    suspend operator fun invoke(downloadUrl: String) {
        get<DownloadAudioMediaUseCase>()(downloadUrl).getOrThrow()
    }
}

fun createIosDownloadAudioMediaUseCaseBridge(): IosDownloadAudioMediaUseCaseBridge =
    IosDownloadAudioMediaUseCaseBridge()
