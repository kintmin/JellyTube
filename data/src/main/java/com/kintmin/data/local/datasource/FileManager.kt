package com.kintmin.data.local.datasource

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class FileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isExistFile(fileName: String, fileType: FileType) = runCatching {
        when (fileType) {
            FileType.Audio -> listOf(Ext.mp3, Ext.m4a, Ext.webm, Ext.opus)
            FileType.Image -> listOf(Ext.webp, Ext.jpg, Ext.jpeg, Ext.png)
        }.forEach { ext ->
            if (getFile("${fileName}.${ext.name}").exists()) {
                return@runCatching true
            }
        }
        false
    }

    fun getFullPath(fileName: String, ext: Ext) = runCatching {
        getFile("${fileName}.${ext}").absolutePath
    }

    fun getFileNameFromPath(path: String): String? {
        val fileName = path.substringAfterLast(".", "")
        return fileName.ifBlank { return null }
    }

    private fun getFile(fileName: String): File {
        val ext = fileName.substringAfterLast(".")
        val dir = getDirectory(Ext.entries.find { it.name == ext }?.fileType)
        return dir.resolve(fileName)
    }

    private fun getDirectory(fileType: FileType?): File {
        return when (fileType) {
            FileType.Audio -> context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            FileType.Image -> context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            null -> null
        } ?: context.filesDir
    }

    enum class FileType {
        Audio,
        Image,
    }

    enum class Ext(val fileType: FileType) {
        mp3(FileType.Audio),
        m4a(FileType.Audio),
        webm(FileType.Audio),
        opus(FileType.Audio),
        jpg(FileType.Image),
        jpeg(FileType.Image),
        png(FileType.Image),
        webp(FileType.Image),
    }
}