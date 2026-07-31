package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

/**
 * 수신이 중단돼 버려진 업로드 스테이징 파일을 정리하고 삭제한 개수를 반환한다.
 * 파일공유 서버가 켜지는 시점에 호출해, 다음 업로드가 없더라도 회수되게 한다.
 */
class CleanupUploadedAudioStagingFilesUseCase(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(): Result<Int> = audioMediaRepository.cleanupUploadedAudioStagingFiles()
}
