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

    suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<String>
    suspend fun fetchLyrics(fileNameWithExt: String): Result<String>

    suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo>

    suspend fun saveUploadedAudio(sourceFileFullPath: String, originalFileName: String): Result<CopiedAudioInfo>

    /**
     * 앱 내부 음원을 기기 공용 저장소로 내보낸다.
     *
     * 디스크 파일명이 UUID라서 공용 저장소 파일명은 [title]/[artist]로 만든다. MediaStore의 TITLE
     * 컬럼이 readOnly라 파일 태그가 없으면 목록 표시명이 파일명에서 파생되므로, 파일명이 알람음/
     * 벨소리 목록에 보일 이름을 사실상 결정한다.
     *
     * [targetList]의 용도마다 해당 공용 디렉토리에 복사본을 하나씩 만든다 — is_alarm 등이 readOnly
     * 컬럼이라 경로 외에는 목록 노출을 제어할 방법이 없다.
     */
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
