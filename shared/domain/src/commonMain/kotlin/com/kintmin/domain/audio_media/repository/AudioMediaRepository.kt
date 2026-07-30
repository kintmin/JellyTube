package com.kintmin.domain.audio_media.repository

import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.lyrics.model.LyricsVariant
import kotlinx.coroutines.flow.Flow

interface AudioMediaRepository {
    suspend fun downloadAudioMedia(downloadUrl: String): Result<DownloadedMedia>
    suspend fun addAudioMedia(
        downloadedAudioMedia: DownloadedMedia,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia>
    suspend fun getAudioMediaBySource(source: String): Result<AudioMedia>
    fun getAudioMediaListFlow(): Flow<List<AudioMedia>>
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileFullPath: String? = null,
        lyricFileFullPath: String? = null,
    ): Result<Unit>

    suspend fun saveImage(imageData: ByteArray): Result<String>

    /** 음원에 TJ 노래방 번호를 연동한다. */
    suspend fun updateKaraokeNumber(id: Int, tjKaraokeNumber: String): Result<Unit>

    /** 음원에 연동된 TJ 노래방 번호를 해제한다. */
    suspend fun clearKaraokeNumber(id: Int): Result<Unit>

    suspend fun saveLyrics(text: String, synced: Boolean): Result<String>
    suspend fun getLyrics(lyricFileFullPath: String): Result<String>
    suspend fun deleteLyrics(id: Int): Result<Unit>

    /** 원본 가사 경로에서 파생한 변형(번역/음차) 파일을 저장한다. */
    suspend fun saveVariantLyrics(baseLyricFileFullPath: String, variant: LyricsVariant, text: String): Result<Unit>

    /** 원본 가사 경로에서 파생한 변형 파일을 읽는다. 파일이 없으면 null. */
    suspend fun getVariantLyrics(baseLyricFileFullPath: String, variant: LyricsVariant): Result<String?>

    /** 원본 가사 경로에서 파생한 변형 파일을 삭제한다. 파일이 없으면 무시한다. */
    suspend fun deleteVariantLyrics(baseLyricFileFullPath: String, variant: LyricsVariant): Result<Unit>

    suspend fun importSharedAudio(
        contentUriString: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia>

    /**
     * 업로드 수신용 스테이징 파일 경로를 발급한다.
     * 발급된 경로는 오디오 보관 디렉터리와 같은 볼륨이므로 [importUploadedAudio]는 복사 없이 이동만 한다.
     * 수신 측은 이 경로에 바이트를 흘려 쓰면서 SHA-256을 함께 계산한다.
     */
    suspend fun createUploadedAudioStagingFilePath(): Result<String>

    /** [stagingFilePath]의 파일을 오디오 보관 디렉터리로 이동시켜 등록한다. */
    suspend fun importUploadedAudio(
        stagingFilePath: String,
        sha256Hex: String,
        originalFileName: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia>

    suspend fun deleteDownloadedFile(downloadedAudioMedia: DownloadedMedia): Result<Unit>
    suspend fun deleteAudioMedia(id: Int): Result<Unit>

    suspend fun deleteOrphanAudioMedia(): Result<List<AudioMedia>>
    suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit>
    suspend fun listAudioAndImageFileFullPaths(): Result<List<String>>
}
