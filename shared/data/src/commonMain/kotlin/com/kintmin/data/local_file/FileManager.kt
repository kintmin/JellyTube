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
    suspend fun createUploadStagingFilePath(): Result<String>

    /**
     * 보관 기간이 지난 스테이징 파일을 삭제하고 삭제한 개수를 반환한다.
     *
     * 수신 도중 프로세스가 죽으면 스테이징 파일이 남는데, [listAudioAndImageFileFullPaths] 기반
     * 고아 파일 정리는 하위 디렉터리를 훑지 않으므로 이 경로가 유일한 회수 수단이다.
     */
    suspend fun cleanupExpiredUploadStagingFiles(): Result<Int>

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
