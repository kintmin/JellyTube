package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.Ext

interface FileManager {
    fun getFileNameWithExt(fileNameWithExt: String): Result<String>
    fun getFullPathWithExt(fileName: String, ext: Ext): Result<String>
    fun getFullPathWithExt(fileName: String, extName: String): Result<String>
    fun getFullPathWithExt(fileNameWithExt: String): Result<String>

    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<Ext>

    suspend fun deleteFile(fileName: String, ext: Ext): Result<Unit>
    fun clearDiskCache(): Result<Unit>
}