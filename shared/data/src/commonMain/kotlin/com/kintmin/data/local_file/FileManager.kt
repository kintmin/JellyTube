package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo

interface FileManager {
    fun getFileNameWithExt(fileFullPath: String): Result<String>

    fun getAudioDownloadBasePath(fileName: String): Result<String>

    fun getAudioFileFullPath(fileNameWithExt: String): Result<String>
    fun getImageFileFullPath(fileNameWithExt: String): Result<String>
    fun getLyricFileFullPath(fileNameWithExt: String): Result<String>

    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<String>

    suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<String>
    suspend fun fetchLyrics(fileNameWithExt: String): Result<String>

    suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo>

    /**
     * 업로드 수신용 스테이징 파일 경로를 발급한다.
     * 오디오 디렉터리와 같은 볼륨에 두므로 [commitUploadedAudio]가 복사 없이 이동만으로 끝난다.
     */
    fun createUploadStagingFilePath(): Result<String>

    /** 스테이징 파일을 오디오 디렉터리로 이동시키고 메타데이터를 추출한다. */
    suspend fun commitUploadedAudio(
        stagingFilePath: String,
        sha256Hex: String,
        originalFileName: String,
    ): Result<CopiedAudioInfo>

    suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit>

    suspend fun listAudioAndImageFileFullPaths(): Result<List<String>>

    fun clearDiskCache(): Result<Unit>

    suspend fun appendAppLog(date: String, line: String): Result<Unit>
    suspend fun fetchAppLogDateList(): Result<List<String>>
    suspend fun fetchAppLogLineList(date: String): Result<List<String>>
}
