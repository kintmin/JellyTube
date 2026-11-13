package com.kintmin.domain.file_share.usecase

import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.file_share.repository.PcFileShareServerRepository

class StartPcFileShareServerUseCase(
    private val repository: PcFileShareServerRepository,
    private val createPcUploadHandler: CreatePcUploadHandlerUseCase,
) {
    operator fun invoke(onUploadImported: (ImportedAudioMediaResult) -> Unit) =
        repository.start(createPcUploadHandler(onUploadImported))
}
