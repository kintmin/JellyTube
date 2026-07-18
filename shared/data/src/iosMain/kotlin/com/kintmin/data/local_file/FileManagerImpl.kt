package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
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

    override fun getAudioDownloadBasePath(fileName: String): Result<String> = runCatching {
        audioDir() + "/$fileName"
    }

    override fun getAudioFileFullPath(fileNameWithExt: String): Result<String> = runCatching {
        audioDir() + "/$fileNameWithExt"
    }

    override fun getImageFileFullPath(fileNameWithExt: String): Result<String> = runCatching {
        imageDir() + "/$fileNameWithExt"
    }

    override fun getLyricFileFullPath(fileNameWithExt: String): Result<String> = runCatching {
        lyricDir() + "/$fileNameWithExt"
    }

    override suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<String> = unsupported()

    override suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<String> = unsupported()

    override suspend fun fetchLyrics(fileNameWithExt: String): Result<String> = unsupported()

    override suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo> = unsupported()

    override suspend fun saveUploadedAudio(bytes: ByteArray, originalFileName: String): Result<CopiedAudioInfo> = unsupported()

    override suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit> = runCatching {
        NSFileManager.defaultManager.removeItemAtPath(fileFullPath, error = null)
    }

    override suspend fun listAudioAndImageFileFullPaths(): Result<List<String>> = unsupported()

    override fun clearDiskCache(): Result<Unit> = runCatching { }

    override suspend fun appendAppLog(date: String, line: String): Result<Unit> = runCatching { }

    override suspend fun fetchAppLogDateList(): Result<List<String>> = runCatching { emptyList() }

    override suspend fun fetchAppLogLineList(date: String): Result<List<String>> = runCatching { emptyList() }

    private fun audioDir(): String = directory(AUDIO_DIR_NAME)

    private fun imageDir(): String = directory(IMAGE_DIR_NAME)

    private fun lyricDir(): String = directory(LYRIC_DIR_NAME)

    private fun directory(dirName: String): String {
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
