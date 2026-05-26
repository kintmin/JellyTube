package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.Ext

interface FileManager {
    fun getFileNameWithExt(fileFullPath: String): Result<String>

    fun getFullPathWithExt(fileName: String, ext: Ext): Result<String>
    fun getFullPathWithExt(fileNameWithExt: String): Result<String>

    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<Ext>

    /**
     * contentUriString 로 지정된 content:// URI의 오디오 파일을 앱 내부 음악 디렉터리로 복사하고,
     * 파일 해시·메타데이터를 포함한 [CopiedAudioInfo]를 반환한다.
     */
    suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo>

    suspend fun deleteFile(fileNameWithExt: String): Result<Unit>
    fun clearDiskCache(): Result<Unit>

    suspend fun appendAppLog(date: String, line: String): Result<Unit>
    suspend fun fetchAppLogDateList(): Result<List<String>>
    suspend fun fetchAppLogLineList(date: String): Result<List<String>>
}
