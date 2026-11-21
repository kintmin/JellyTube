package com.kintmin.domain.playlist_download.usecase

import com.kintmin.domain.audio_media.ParallelDownloader
import com.kintmin.domain.audio_media.model.AgeRestrictedMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.data_migration.SourceKey
import com.kintmin.domain.lyrics.usecase.DownloadLyricsForAudioMediaUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist_download.PlaylistMergeOrder
import com.kintmin.domain.playlist_download.model.PlaylistDownloadFailure
import com.kintmin.domain.playlist_download.model.PlaylistDownloadFailureReason
import com.kintmin.domain.playlist_download.model.PlaylistDownloadProgress
import com.kintmin.domain.playlist_download.model.PlaylistDownloadStage
import com.kintmin.domain.playlist_download.model.PlaylistDownloadSummary
import com.kintmin.domain.youtube.model.YoutubePlaylist
import com.kintmin.domain.youtube.model.YoutubePlaylistEntry
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * 유튜브 재생목록을 같은 이름의 재생목록에 받는다. 받은 곡은 미분류에 두었다가 끝에 유튜브 순서대로 한 번에 담는다.
 * 멈추거나 실패해도 그때까지 받은 곡은 담으므로, 다시 부르면 빠진 곡만 받아 제자리에 끼운다.
 */
class DownloadYoutubePlaylistUseCase(
    private val audioMediaRepository: AudioMediaRepository,
    private val audioTrackRepository: AudioTrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val addNewPlaylistUseCase: AddNewPlaylistUseCase,
    private val downloadAudioMediaUseCase: DownloadAudioMediaUseCase,
    private val downloadLyricsForAudioMediaUseCase: DownloadLyricsForAudioMediaUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        playlist: YoutubePlaylist,
        onProgress: (PlaylistDownloadProgress) -> Unit,
    ): Result<PlaylistDownloadSummary> = runCatching {
        val entries = playlist.entries.distinctBy { it.videoId }
        val owned = ownedIdByKey()
        val (onDevice, notOnDevice) = entries.partition { SourceKey.of(it.url) in owned }
        val (toDownload, unavailable) = notOnDevice.partition { it.isAvailable }

        val resultLock = Mutex()
        val downloaded = mutableMapOf<String, Int>()
        val ageRestricted = mutableSetOf<String>()
        fun idsInOrder() = entries.mapNotNull { owned[SourceKey.of(it.url)] ?: downloaded[it.videoId] }

        onProgress(PlaylistDownloadProgress(PlaylistDownloadStage.DOWNLOAD, 0, toDownload.size))
        val results = try {
            ParallelDownloader.run(
                items = toDownload,
                onDone = { onProgress(PlaylistDownloadProgress(PlaylistDownloadStage.DOWNLOAD, it, toDownload.size)) },
            ) { entry ->
                // 다시 시도해도 받을 수 없으니 실패로 넘겨 재시도를 태우지 않는다.
                val acquired = try {
                    acquire(entry)
                } catch (e: AgeRestrictedMedia) {
                    null
                }
                // 곡 행은 이미 생겼으니 멈추는 중이어도 기록해야 재생목록에 담긴다.
                withContext(NonCancellable) {
                    resultLock.withLock {
                        if (acquired == null) ageRestricted += entry.videoId else downloaded[entry.videoId] = acquired.first.id
                    }
                }
                if (acquired?.second == true) {
                    downloadLyricsForAudioMediaUseCase(
                        audioMediaId = acquired.first.id,
                        title = acquired.first.name,
                        targetDurationSeconds = entry.durationSeconds?.toDouble(),
                    )
                }
            }
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                runCatching { addInOrder(playlist.title, idsInOrder(), onProgress) }
                    .onFailure { logError("멈춘 뒤 재생목록에 담지 못함", it) }
            }
            throw e
        }
        toDownload.zip(results).forEach { (entry, result) ->
            result.onFailure { log("받기 실패: ${entry.title} (${entry.videoId}) $it") }
        }

        val playlistId = withContext(NonCancellable) { addInOrder(playlist.title, idsInOrder(), onProgress) }

        PlaylistDownloadSummary(
            playlistId = playlistId,
            playlistName = playlist.title.trim(),
            downloadedCount = downloaded.size,
            alreadyOnDeviceCount = onDevice.size,
            failures = unavailable.map { PlaylistDownloadFailure(it.title, PlaylistDownloadFailureReason.UNAVAILABLE) } +
                toDownload.filter { it.videoId !in downloaded }.map {
                    val reason = if (it.videoId in ageRestricted) {
                        PlaylistDownloadFailureReason.AGE_RESTRICTED
                    } else {
                        PlaylistDownloadFailureReason.DOWNLOAD_FAILED
                    }
                    PlaylistDownloadFailure(it.title, reason)
                },
        )
    }.onFailure {
        if (it is CancellationException) throw it
        logError("재생목록 받기 중단", it)
    }

    private suspend fun ownedIdByKey(): Map<String, Int> =
        audioMediaRepository.getAudioMediaListFlow().first().associate { SourceKey.of(it.source) to it.id }

    /** 받는 사이 같은 주소로 들어온 곡이 있으면 받지 않고 그 곡을 쓴다. 두 번째 값은 새로 받았는지다. */
    private suspend fun acquire(entry: YoutubePlaylistEntry): Pair<AudioMedia, Boolean> {
        val result = downloadAudioMediaUseCase(entry.url, intoUncategorized = true).getOrElse { error ->
            if (error is AlreadyDownloadedMedia) return audioMediaRepository.getAudioMediaBySource(entry.url).getOrThrow() to false
            throw error
        }
        return result.audioMedia to true
    }

    /** 곡은 맨 뒤에만 담을 수 있어 빠진 곡을 붙인 뒤 유튜브 순서의 제자리로 옮긴다. */
    private suspend fun addInOrder(
        title: String,
        audioMediaIds: List<Int>,
        onProgress: (PlaylistDownloadProgress) -> Unit,
    ): Int? {
        val name = title.trim()
        val existing = playlistRepository.getAllPlaylistFlow().first()
            .firstOrNull { it.type == PlaylistType.USER && it.name.trim() == name }
        if (existing == null && audioMediaIds.isEmpty()) return null
        val playlistId = existing?.id ?: addNewPlaylistUseCase(name).getOrThrow()
        val youtubeOrder = audioMediaIds.distinct()
        onProgress(PlaylistDownloadProgress(PlaylistDownloadStage.ADD_TO_PLAYLIST, 0, youtubeOrder.size))
        val before = trackIds(playlistId)
        val toAdd = youtubeOrder.filter { it !in before }
        if (toAdd.isNotEmpty()) {
            audioTrackRepository.addCustomAudioTrack(playlistId, toAdd).getOrThrow()
            val target = PlaylistMergeOrder.of(before, youtubeOrder)
            if (target != before + toAdd) audioTrackRepository.reorderTracks(playlistId, target).getOrThrow()
        }
        onProgress(PlaylistDownloadProgress(PlaylistDownloadStage.ADD_TO_PLAYLIST, youtubeOrder.size, youtubeOrder.size))
        return playlistId
    }

    private suspend fun trackIds(playlistId: Int): List<Int> =
        audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId).first()
            .sortedBy { it.audioTrack.trackSequence }
            .map { it.audioMedia.id }

    private fun log(message: String) {
        appLog.sendDebugLog(DebugLog("DownloadYoutubePlaylistUseCase", message))
    }

    private fun logError(message: String, error: Throwable) {
        appLog.sendErrorLog(ErrorLog("DownloadYoutubePlaylistUseCase", message, error))
    }
}
