package com.kintmin.data.local_file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.local_file.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FileManagerImpl @Inject constructor(
    private val appContext: Context,
) : FileManager {

    private companion object {

        const val LOG_DIR_NAME = "app_logs"
        const val MAX_LOG_FILE_COUNT = 14
    }

    override fun getFileNameWithExt(fileFullPath: String) = runCatching {
        File(fileFullPath).name
    }

    override fun getFullPathWithExt(fileNameWithExt: String) = runCatching {
        val (_, ext) = extractExtFromFileName(fileNameWithExt)
        getDirectory(ext.fileType).resolve(fileNameWithExt).absolutePath
    }

    override fun getFullPathWithExt(fileName: String, ext: Ext) = runCatching {
        val fileNameWithExt = "$fileName.$ext"
        getDirectory(ext.fileType).resolve(fileNameWithExt).absolutePath
    }

    override suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String) =
        runCatching {
            withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                val (targetExt, format) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Ext.WEBP to Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Ext.JPEG to Bitmap.CompressFormat.JPEG
                }

                val outputFile = getDirectory(FileType.Image).resolve("$fileName.$targetExt")
                FileOutputStream(outputFile).use { outputStream ->
                    bitmap.compress(format, 60, outputStream)
                }

                targetExt
            }
        }

    override suspend fun deleteFile(fileNameWithExt: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val (_, ext) = extractExtFromFileName(fileNameWithExt)
            val file = getDirectory(ext.fileType).resolve(fileNameWithExt)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override fun clearDiskCache(): Result<Unit> = runCatching {
        appContext.cacheDir.deleteRecursively()
    }

    override suspend fun appendAppLog(date: String, line: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val file = getLogDirectory().resolve("$date.log")
            file.appendText("$line\n")
            cleanupOldLogFile()
        }
    }

    override suspend fun fetchAppLogDateList(): Result<List<String>> = runCatching {
        withContext(Dispatchers.IO) {
            getLogDirectory()
                .listFiles()
                ?.filter { file -> file.isFile && file.extension == "log" }
                ?.map { file -> file.nameWithoutExtension }
                ?.sortedDescending()
                ?: emptyList()
        }
    }

    override suspend fun fetchAppLogLineList(date: String): Result<List<String>> = runCatching {
        withContext(Dispatchers.IO) {
            val targetFile = getLogDirectory().resolve("$date.log")
            if (!targetFile.exists()) return@withContext emptyList()
            targetFile.readLines().asReversed()
        }
    }

    private fun extractExtFromFileName(fileNameWithExt: String): Pair<String, Ext> {
        val lastDotIndex = fileNameWithExt.lastIndexOf(".")
        if (lastDotIndex == -1) {
            throw Exception("파일명에서 파일 확장자를 찾을 수 없습니다.")
        }

        val fileName = fileNameWithExt.substring(0, lastDotIndex)
        val extName = fileNameWithExt.substring(lastDotIndex + 1)
        val ext = extractExt(extName)
        return fileName to ext
    }

    private fun extractExt(extName: String): Ext {
        return Ext.entries.find { it.name.equals(extName, ignoreCase = true) } ?: throw Exception("올바르지 않은 확장자입니다.")
    }

    private fun getDirectory(fileType: FileType): File {
        val dir = when (fileType) {
            FileType.Audio -> appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            FileType.Image -> appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }
        return dir?.takeIf { it.exists() || it.mkdirs() }
            ?: throw Exception("디렉토리를 찾을 수 없습니다.")
    }

    private fun getLogDirectory(): File {
        val dir = appContext.filesDir.resolve(LOG_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun cleanupOldLogFile() {
        val allLogFile = getLogDirectory()
            .listFiles()
            ?.filter { file ->
                file.isFile && file.extension == "log"
            }
            ?.sortedByDescending { file -> file.nameWithoutExtension }
            ?: return

        allLogFile.drop(MAX_LOG_FILE_COUNT).forEach { file ->
            file.delete()
        }
    }
}
