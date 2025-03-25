package com.kintmin.localfile

import com.kintmin.localfile.model.Ext

interface FileManager {
    fun getFileNameWithExt(fullPath: String): Result<String>
    fun getFullPathWithExt(fileName: String, ext: Ext): Result<String>
    fun getFullPathWithExt(fileNameWithExt: String): Result<String>

    /**
     * Return: 저장 성공한 파일 FullPath
     */
    suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<String>

    suspend fun deleteFile(fileName: String, ext: Ext): Result<Unit>
    fun clearDiskCache(): Result<Unit>
}