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

    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<String>

    /**
     * 이미지 파일을 새 이름으로 복사한다. 반환값은 확장자.
     *
     * 편집본을 새 음원으로 저장할 때 원본 썸네일을 물려주는 데 쓴다. 파일명을 공유하면
     * 한쪽 음원을 지울 때 다른 쪽 썸네일까지 사라지므로 반드시 복사해야 한다.
     */
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
