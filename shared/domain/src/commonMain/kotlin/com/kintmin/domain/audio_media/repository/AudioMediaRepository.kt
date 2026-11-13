package com.kintmin.domain.audio_media.repository

import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.AudioMediaExportStatus
import com.kintmin.domain.audio_media.model.DownloadedAudioFile
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.model.DownloadedYoutubeAudio
import com.kintmin.domain.lyrics.model.LyricsVariant
import kotlinx.coroutines.flow.Flow

interface AudioMediaRepository {
    suspend fun downloadYoutubeAudio(downloadUrl: String): Result<DownloadedYoutubeAudio>

    /** 받은 썸네일을 [fileName] 으로 압축해 저장하고 확장자 붙은 이름을 돌려준다. */
    suspend fun saveThumbnail(thumbnailUrl: String, fileName: String): Result<String>

    /**
     * 이 기기에서 재생할 수 있는 형식으로 음원 파일만 받는다. 행을 만들지 않으므로 쓰지 않게 되면 호출자가 지운다.
     * 앱 프로세스 안에서 받으므로 앱이 떠 있는 동안 써야 한다.
     */
    suspend fun downloadAudioFile(downloadUrl: String): Result<DownloadedAudioFile>
    suspend fun addAudioMedia(
        downloadedAudioMedia: DownloadedMedia,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia>
    suspend fun getAudioMediaBySource(source: String): Result<AudioMedia>

    /** 같은 출처를 받는 중이면 false. 받는 일은 화면·작업 하나보다 오래 살아서 앱에 하나인 여기서 센다. */
    fun reserveDownload(source: String): Boolean

    fun releaseDownload(source: String)
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

    suspend fun importUploadedAudio(
        uploadedFileFullPath: String,
        originalFileName: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia>

    suspend fun deleteDownloadedFile(downloadedAudioMedia: DownloadedMedia): Result<Unit>
    suspend fun deleteAudioMedia(id: Int): Result<Unit>

    suspend fun deleteOrphanAudioMedia(): Result<List<AudioMedia>>
    suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit>
    suspend fun listAudioAndImageFileFullPaths(): Result<List<String>>

    /** 음원 파일 확장자를 내용에 맞춘다. 바로잡은 파일 수를 돌려준다. */
    suspend fun correctAudioFileExtensions(): Result<Int>

    suspend fun exportAudioMediaToDevice(
        audioFileFullPath: String,
        title: String,
        artist: String,
        targetList: List<AudioExportTarget>,
    ): Result<AudioMediaExportStatus>
}
