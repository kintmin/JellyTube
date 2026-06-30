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
        // 1) 어떤 track에도 연결되지 않은 AUDIO_MEDIA를 단일 트랜잭션으로 전수 삭제
        val orphanMediaList = audioMediaRepository.deleteOrphanAudioMedia().getOrThrow()
        var anomalyCount = orphanMediaList.size

        // 커밋 후 orphan row가 참조하던 파일 삭제
        orphanMediaList.forEach { media ->
            deleteFileSafely(media.audioFileFullPath.toFileName())
            media.imageFileFullPath?.let { deleteFileSafely(it.toFileName()) }
        }

        // 2) DB 어디에도 매칭되지 않는 고아 파일 삭제
        // 화이트리스트 = 남아있는 모든 음원의 오디오/이미지 파일명 ∪ 플레이리스트 커스텀 이미지 파일명
        val referencedFileNames = buildSet {
            audioMediaRepository.getAudioMediaListFlow().first().forEach { media ->
                add(media.audioFileFullPath.toFileName())
                media.imageFileFullPath?.let { add(it.toFileName()) }
            }
            playlistRepository.getAllPlaylistFlow().first().forEach { playlist ->
                playlist.imageFileFullPath?.let { add(it.toFileName()) }
            }
        }
        val orphanFileNames = audioMediaRepository.listAudioAndImageFileNames().getOrThrow()
            .filterNot { it in referencedFileNames }
        orphanFileNames.forEach { deleteFileSafely(it) }
        anomalyCount += orphanFileNames.size

        anomalyCount
    }

    private suspend fun deleteFileSafely(fileNameWithExt: String) {
        audioMediaRepository.deleteFile(fileNameWithExt).onFailure {
            appLog.sendDebugLog(DebugLog("CleanupAnomalousData", "파일 삭제 실패: $fileNameWithExt / $it"))
        }
    }

    private fun String.toFileName(): String = substringAfterLast("/")
}
