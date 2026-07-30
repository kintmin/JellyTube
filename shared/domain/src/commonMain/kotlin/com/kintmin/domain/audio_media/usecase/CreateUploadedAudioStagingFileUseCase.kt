package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

/**
 * 업로드 수신용 스테이징 파일 경로를 발급한다.
 * 수신 측이 이 경로로 바로 스트리밍해야 [ImportUploadedAudioMediaUseCase]가 복사 없이 이동만으로 끝난다.
 */
class CreateUploadedAudioStagingFileUseCase(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(): Result<String> = audioMediaRepository.createUploadedAudioStagingFilePath()
}
