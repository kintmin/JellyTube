package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlinx.coroutines.flow.first

class CleanupAnomalousDataUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(): Result<Int> = runCatching {
        appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "track 연결 없는 음원 정리 시작"))

        // 1) 어떤 track에도 연결되지 않은 AUDIO_MEDIA를 단일 트랜잭션으로 전수 삭제
        val orphanMediaList = audioMediaRepository.deleteOrphanAudioMedia().getOrThrow()
        orphanMediaList.forEach {
            appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "음원 row 삭제 완료: $it"))
        }

        // 커밋 후 orphan row가 참조하던 파일 삭제
        orphanMediaList.forEach { media ->
            deleteFileSafely(media.audioFileFullPath)
            media.imageFileFullPath?.let { deleteFileSafely(it) }
        }

        appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "track 연결 없는 음원 정리 완료"))

        appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "DB 연결 없는 파일 정리 시작"))

        // 2) DB 어디에도 매칭되지 않는 고아 파일 삭제
        // 화이트리스트 = 남아있는 모든 음원의 오디오/이미지 전체경로 ∪ 플레이리스트 커스텀 이미지 전체경로
        val referencedFileFullPaths = buildSet {
            audioMediaRepository.getAudioMediaListFlow().first().forEach { media ->
                add(media.audioFileFullPath)
                media.imageFileFullPath?.let { add(it) }
            }
            playlistRepository.getAllPlaylistFlow().first().forEach { playlist ->
                playlist.imageFileFullPath?.let { add(it) }
            }
        }
        val orphanFileFullPaths = audioMediaRepository.listAudioAndImageFileFullPaths().getOrThrow()
            .filterNot { it in referencedFileFullPaths }
        orphanFileFullPaths.forEach { deleteFileSafely(it) }

        appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "DB 연결 없는 파일 정리 완료"))

        val anomalyCount = orphanMediaList.size + orphanFileFullPaths.size
        anomalyCount
    }

    private suspend fun deleteFileSafely(fileFullPath: String) {
        audioMediaRepository.deleteFileAtFullPath(fileFullPath).onSuccess {
            appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "파일 삭제 완료: $fileFullPath"))
        }.onFailure {
            appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "파일 삭제 실패: $fileFullPath / $it"))
        }
    }
}
