package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.Ext

interface FileManager {
    fun getFileNameWithExt(fileFullPath: String): Result<String>

    fun getFullPathWithExt(fileName: String, ext: Ext): Result<String>
    fun getFullPathWithExt(fileNameWithExt: String): Result<String>

    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<Ext>

    suspend fun deleteFile(fileNameWithExt: String): Result<Unit>
    fun clearDiskCache(): Result<Unit>

    suspend fun appendAppLog(date: String, line: String): Result<Unit>
    suspend fun fetchAppLogDateList(): Result<List<String>>
    suspend fun fetchAppLogLineList(date: String): Result<List<String>>
}
