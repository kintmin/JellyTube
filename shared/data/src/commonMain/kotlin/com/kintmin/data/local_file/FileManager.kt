package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.ExportedAudioInfo
import com.kintmin.domain.audio_media.model.AudioExportTarget

interface FileManager {
    fun getFileNameWithExt(fileFullPath: String): Result<String>

    fun getAudioDownloadBasePath(fileName: String): Result<String>

    fun getAudioFileFullPath(fileNameWithExt: String): Result<String>
    fun getImageFileFullPath(fileNameWithExt: String): Result<String>
    fun getLyricFileFullPath(fileNameWithExt: String): Result<String>

    /** 데이터 이전으로 받는 파일을 반영 전까지 두는 곳. 음원 디렉터리와 같은 볼륨이어야 옮기기가 원자적이다. */
    fun getMigrationStagingDirPath(): Result<String>

    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<String>

    suspend fun copyImageFile(sourceFileNameWithExt: String, targetFileName: String): Result<String>

    suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<String>
    suspend fun fetchLyrics(fileNameWithExt: String): Result<String>

    suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo>

    suspend fun saveUploadedAudio(sourceFileFullPath: String, originalFileName: String): Result<CopiedAudioInfo>

    suspend fun exportAudioToPublicStorage(
        sourceAudioFileFullPath: String,
        title: String,
        artist: String?,
        targetList: List<AudioExportTarget>,
    ): Result<ExportedAudioInfo>

    suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit>

    suspend fun listAudioAndImageFileFullPaths(): Result<List<String>>

    fun clearDiskCache(): Result<Unit>

    suspend fun appendAppLog(date: String, line: String): Result<Unit>
    suspend fun fetchAppLogDateList(): Result<List<String>>
    suspend fun fetchAppLogLineList(date: String): Result<List<String>>
}
