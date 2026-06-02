package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.Ext

interface FileManager {
    fun getFileNameWithExt(fileFullPath: String): Result<String>

    fun getFullPathWithExt(fileName: String, ext: Ext): Result<String>
    fun getFullPathWithExt(fileNameWithExt: String): Result<String>

    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<Ext>

    /**
     * contentUriString ë¡?ì§€?•ëœ content:// URI???¤ë””???Œì¼?????´ë? ?Œì•… ?”ë ‰?°ë¦¬ë¡?ë³µì‚¬?˜ê³ ,
     * ?Œì¼ ?´ì‹œÂ·ë©”í??°ì´?°ë? ?¬í•¨??[CopiedAudioInfo]ë¥?ë°˜í™˜?œë‹¤.
     */
    suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo>

    /**
     * ?ì‹œ ë°”ì´??ë°°ì—´(HTTP ?…ë¡œ???????¤ë””???Œì¼ë¡??€?¥í•˜ê³?[CopiedAudioInfo]ë¥?ë°˜í™˜?œë‹¤.
     */
    suspend fun saveUploadedAudio(bytes: ByteArray, originalFileName: String): Result<CopiedAudioInfo>

    suspend fun deleteFile(fileNameWithExt: String): Result<Unit>
    fun clearDiskCache(): Result<Unit>

    suspend fun appendAppLog(date: String, line: String): Result<Unit>
    suspend fun fetchAppLogDateList(): Result<List<String>>
    suspend fun fetchAppLogLineList(date: String): Result<List<String>>
}
