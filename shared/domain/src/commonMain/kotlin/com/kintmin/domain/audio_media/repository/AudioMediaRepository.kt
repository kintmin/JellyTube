package com.kintmin.domain.audio_media.repository

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import kotlinx.coroutines.flow.Flow

interface AudioMediaRepository {
    suspend fun downloadAudioMedia(downloadUrl: String): Result<DownloadedMedia>
    suspend fun addAudioMedia(
        downloadedAudioMedia: DownloadedMedia,
        playlistIdOnDownload: Int,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<Pair<AudioMedia, Int>>
    suspend fun getAudioMediaBySource(source: String): Result<AudioMedia>
    fun getAudioMediaListFlow(): Flow<List<AudioMedia>>
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileFullPath: String? = null,
    ): Result<Unit>

    suspend fun saveImage(imageData: ByteArray): Result<String>

    /**
     * content URI로 공유된 오디오 파일을 내부 저장소로 복사하고 DB에 저장한다.
     * source는 "quickShare://sha256/<hex>" 형태로 저장된다.
     */
    suspend fun importSharedAudio(
        contentUriString: String,
        playlistIdOnDownload: Int,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<Pair<AudioMedia, Int>>

    /**
     * HTTP 스트림으로 수신된 바이트 배열을 내부 저장소에 저장하고 DB에 추가한다.
     * source는 "fileShare://sha256/<hex>" 형태로 저장된다.
     */
    suspend fun importUploadedAudio(
        bytes: ByteArray,
        originalFileName: String,
        playlistIdOnDownload: Int,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<Pair<AudioMedia, Int>>

    suspend fun deleteDownloadedFile(downloadedAudioMedia: DownloadedMedia): Result<Unit>
    suspend fun deleteAudioMedia(id: Int): Result<Unit>
}
