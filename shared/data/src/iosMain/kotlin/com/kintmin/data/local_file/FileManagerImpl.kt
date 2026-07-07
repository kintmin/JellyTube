package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.local_file.model.FileType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal class FileManagerImpl : FileManager {

    private companion object {
        const val IMAGE_DIR_NAME = "images"
        const val AUDIO_DIR_NAME = "audios"
        const val LYRIC_DIR_NAME = "lyrics"
    }

    override fun getFileNameWithExt(fileFullPath: String): Result<String> = runCatching {
        fileFullPath.substringAfterLast("/")
    }

    override fun getFullPathWithExt(fileName: String, ext: Ext): Result<String> = runCatching {
        directory(ext.fileType) + "/$fileName.$ext"
    }

    override fun getFullPathWithExt(fileNameWithExt: String): Result<String> = runCatching {
        val ext = extractExt(fileNameWithExt.substringAfterLast("."))
        directory(ext.fileType) + "/$fileNameWithExt"
    }

    override suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<Ext> = unsupported()

    override suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<Ext> = unsupported()

    override suspend fun fetchLyrics(fileNameWithExt: String): Result<String> = unsupported()

    override suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo> = unsupported()

    override suspend fun saveUploadedAudio(bytes: ByteArray, originalFileName: String): Result<CopiedAudioInfo> = unsupported()

    override suspend fun deleteFile(fileNameWithExt: String): Result<Unit> = runCatching {
        val path = getFullPathWithExt(fileNameWithExt).getOrThrow()
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }

    override suspend fun listAudioAndImageFileNames(): Result<List<String>> = unsupported()

    override fun clearDiskCache(): Result<Unit> = runCatching { }

    override suspend fun appendAppLog(date: String, line: String): Result<Unit> = runCatching { }

    override suspend fun fetchAppLogDateList(): Result<List<String>> = runCatching { emptyList() }

    override suspend fun fetchAppLogLineList(date: String): Result<List<String>> = runCatching { emptyList() }

    private fun extractExt(extName: String): Ext {
        return Ext.entries.find { it.name.equals(extName, ignoreCase = true) }
            ?: error("올바르지 않은 파일 확장자입니다.")
    }

    private fun directory(fileType: FileType): String {
        val dirName = when (fileType) {
            FileType.Audio -> AUDIO_DIR_NAME
            FileType.Image -> IMAGE_DIR_NAME
            FileType.Lyric -> LYRIC_DIR_NAME
        }
        return documentDirectory().appendPathComponent(dirName).also(::ensureDirectory)
    }

    private fun documentDirectory(): String {
        val documentDirectory: NSURL = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        ) ?: error("Document directory is unavailable")

        return documentDirectory.path ?: error("Document directory path is unavailable")
    }

    private fun ensureDirectory(path: String) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }

    private fun <T> unsupported(): Result<T> = Result.failure(
        UnsupportedOperationException("iOS에서는 이번 범위에서 지원하지 않는 파일 작업입니다.")
    )
}

private fun String.appendPathComponent(component: String): String = trimEnd('/') + "/" + component
