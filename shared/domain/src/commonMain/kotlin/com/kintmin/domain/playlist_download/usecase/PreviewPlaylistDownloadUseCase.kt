package com.kintmin.domain.playlist_download.usecase

import com.kintmin.domain.audio_media.DownloadSizeEstimate
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.data_migration.SourceKey
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.playlist_download.model.PlaylistDownloadPreview
import com.kintmin.domain.youtube.YoutubeUrl
import kotlinx.coroutines.flow.first

class PreviewPlaylistDownloadUseCase(
    private val audioMediaRepository: AudioMediaRepository,
    private val playlistRepository: PlaylistRepository,
) {
    /** 영상이 섞인 주소도 재생목록 페이지 주소로 바꿔 읽는다. 그래야 yt-dlp 가 그 영상이 아니라 목록을 읽는다. */
    suspend operator fun invoke(url: String): Result<PlaylistDownloadPreview> = suspendRunCatching {
        val playlistId = requireNotNull(YoutubeUrl.playlistIdOf(url)) { "재생목록 주소가 아닙니다: $url" }
        val playlist = audioMediaRepository.fetchYoutubePlaylist(YoutubeUrl.playlistPageUrlOf(playlistId)).getOrThrow()
        val ownedKeys = audioMediaRepository.getAudioMediaListFlow().first().map { SourceKey.of(it.source) }.toSet()
        val entries = playlist.entries.distinctBy { it.videoId }
        val (owned, notOwned) = entries.partition { SourceKey.of(it.url) in ownedKeys }
        val (toDownload, unavailable) = notOwned.partition { it.isAvailable }
        PlaylistDownloadPreview(
            playlist = playlist,
            totalCount = entries.size,
            alreadyOnDeviceCount = owned.size,
            downloadCount = toDownload.size,
            unavailableCount = unavailable.size,
            estimatedDownloadBytes = toDownload.sumOf { DownloadSizeEstimate.bytesFor(it.durationSeconds) },
            mergesIntoExistingPlaylist = playlistRepository.getAllPlaylistFlow().first()
                .any { it.type == PlaylistType.USER && it.name.trim() == playlist.title.trim() },
        )
    }
}
