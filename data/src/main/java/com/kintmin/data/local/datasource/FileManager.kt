package com.kintmin.data.local.datasource

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class FileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getFileNameWithExt(fullPath: String) = runCatching {
        File(fullPath).name
    }

    fun getFullPathWithExt(fileName: String, ext: Ext) = runCatching {
        getDirectory(ext.fileType).resolve("$fileName.$ext").absolutePath
    }

    fun getFullPathWithExt(fileNameWithExt: String) = runCatching {
        val (_, ext) = extractExt(fileNameWithExt)
        getDirectory(ext.fileType).resolve(fileNameWithExt).absolutePath
    }

    fun saveImageWithCompression(imageData: ByteArray, fileName: String) = runCatching {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        val outputFile =
            getDirectory(FileType.Image).resolve(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "$fileName.${Ext.WEBP}"
                } else {
                    "$fileName.${Ext.JPG}"
                }
            )

        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.JPEG
        }

        FileOutputStream(outputFile).use { outputStream ->
            bitmap.compress(format, 60, outputStream)
        }

        outputFile.absolutePath
    }

    fun deleteFile(fileName: String, ext: Ext) = runCatching {
        val file = getDirectory(ext.fileType).resolve("$fileName.$ext")
        if (file.exists()) {
            file.delete()
        }
    }

    fun clearCache() : Result<Unit> = runCatching {
        context.cacheDir.deleteRecursively()
        File(context.cacheDir.parentFile, "app_webview").deleteRecursively()
    }

    private fun extractExt(fileNameWithExt: String): Pair<String, Ext> {
        val lastDotIndex = fileNameWithExt.lastIndexOf(".")
        if (lastDotIndex <= 0 || lastDotIndex == fileNameWithExt.length - 1) {
            throw Exception("파일 확장자를 찾을 수 없습니다.")
        }

        val fileName = fileNameWithExt.substring(0, lastDotIndex)
        val extName = fileNameWithExt.substring(lastDotIndex + 1)

        return Ext.entries.find { it.name.equals(extName, ignoreCase = true) }?.let { ext ->
            fileName to ext
        } ?: throw Exception("올바르지 않은 확장자입니다.")
    }

    private fun getDirectory(fileType: FileType): File {
        val dir = when (fileType) {
            FileType.Audio -> context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            FileType.Image -> context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }
        return dir?.takeIf { it.exists() || it.mkdirs() }
            ?: throw Exception("getDirectory: 디렉토리를 찾을 수 없습니다.")
    }

    enum class FileType {
        Audio,
        Image,
    }

    enum class Ext(val fileType: FileType) {
        MP3(FileType.Audio),
        M4A(FileType.Audio),
        WEBM(FileType.Audio),
        OPUS(FileType.Audio),
        JPG(FileType.Image),
        JPEG(FileType.Image),
        PNG(FileType.Image),
        WEBP(FileType.Image),
        ;

        override fun toString() = name.lowercase()
    }
}