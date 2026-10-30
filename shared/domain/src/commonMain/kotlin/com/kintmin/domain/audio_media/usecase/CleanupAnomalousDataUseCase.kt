package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlinx.coroutines.flow.first

class CleanupAnomalousDataUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val audioEditRepository: AudioEditRepository,
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
            media.editedAudioFileFullPath?.let { deleteFileSafely(it) }
            media.imageFileFullPath?.let { deleteFileSafely(it) }
        }

        appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "track 연결 없는 음원 정리 완료"))

        // 렌더 중이거나 DB 연결 직전인 편집본·썸네일은 아직 참조가 없어 고아로 보인다
        if (audioEditRepository.observeSavingAudioMediaIds().first().isNotEmpty()) {
            appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "편집본 저장 중이라 DB 연결 없는 파일 정리 건너뜀"))
            return@runCatching orphanMediaList.size
        }

        appLog.sendDebugLog(DebugLog("CleanupAnomalousDataUseCase", "DB 연결 없는 파일 정리 시작"))

        // 2) DB 어디에도 매칭되지 않는 고아 파일 삭제
        // 화이트리스트 = 남아있는 모든 음원의 오디오(원본+편집본)/이미지 전체경로 ∪ 플레이리스트 커스텀 이미지 전체경로
        val referencedFileFullPaths = buildSet {
            audioMediaRepository.getAudioMediaListFlow().first().forEach { media ->
                add(media.audioFileFullPath)
                // 편집본도 오디오 디렉토리에 있어 파일 목록에 잡힌다. 빠뜨리면 정리 때 삭제된다.
                media.editedAudioFileFullPath?.let { add(it) }
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
