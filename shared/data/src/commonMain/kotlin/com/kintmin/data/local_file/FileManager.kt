package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.Ext

interface FileManager {
    fun getFileNameWithExt(fileFullPath: String): Result<String>

    fun getFullPathWithExt(fileName: String, ext: Ext): Result<String>
    fun getFullPathWithExt(fileNameWithExt: String): Result<String>

    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<Ext>

    /**
     * contentUriString 에서 지정된 content:// URI의 오디오 파일을 내부 앱 디렉토리로 복사하고,
     * 파일 이름·메타데이터를 포함한 [CopiedAudioInfo]를 반환한다.
     */
    suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo>

    /**
     * 임시 바이트 배열(HTTP 스트림 등)의 오디오 파일을 저장하고 [CopiedAudioInfo]를 반환한다.
     */
    suspend fun saveUploadedAudio(bytes: ByteArray, originalFileName: String): Result<CopiedAudioInfo>

    suspend fun deleteFile(fileNameWithExt: String): Result<Unit>
    fun clearDiskCache(): Result<Unit>

    suspend fun appendAppLog(date: String, line: String): Result<Unit>
    suspend fun fetchAppLogDateList(): Result<List<String>>
    suspend fun fetchAppLogLineList(date: String): Result<List<String>>
}
