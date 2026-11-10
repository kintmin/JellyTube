package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.ExportedAudioInfo
import com.kintmin.domain.audio_media.model.AudioExportTarget
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.create
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.dataUsingEncoding
import platform.Foundation.seekToEndOfFile
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeData
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
internal class FileManagerImpl : FileManager {

    private companion object {
        const val IMAGE_DIR_NAME = "images"
        const val AUDIO_DIR_NAME = "audios"
        const val LYRIC_DIR_NAME = "lyrics"
        const val LOG_DIR_NAME = "app_logs"

        /** Android 와 같은 보존 기간. 날짜 파일 14개를 넘으면 오래된 것부터 지운다. */
        const val MAX_LOG_FILE_COUNT = 14

        /**
         * Android 는 WebP 60 으로 줄인다. iOS 에는 공개 WebP 인코더가 없어 JPEG 로 간다.
         * 품질 값은 같은 뜻(0~1 스케일)이고, 확장자는 DB 가 파일별로 들고 있어 달라도 된다.
         */
        const val IMAGE_COMPRESSION_QUALITY = 0.6
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

    override suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val image = UIImage(data = imageData.toNSData()) ?: error("이미지를 해석할 수 없습니다.")
            val compressed = UIImageJPEGRepresentation(image, IMAGE_COMPRESSION_QUALITY)
                ?: error("이미지를 압축할 수 없습니다.")

            val ext = "jpg"
            if (!compressed.writeToFile(imageDir() + "/$fileName.$ext", atomically = true)) {
                error("이미지를 저장할 수 없습니다.")
            }
            ext
        }
    }

    override suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val ext = if (synced) "lrc" else "txt"
            text.writeToFileAtPath(lyricDir() + "/$fileName.$ext")
            ext
        }
    }

    override suspend fun fetchLyrics(fileNameWithExt: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            NSString.stringWithContentsOfFile(
                path = lyricDir() + "/$fileNameWithExt",
                encoding = NSUTF8StringEncoding,
                error = null,
            ) ?: error("가사 파일을 찾을 수 없습니다.")
        }
    }

    override suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo> = unsupported()

    override suspend fun saveUploadedAudio(sourceFileFullPath: String, originalFileName: String): Result<CopiedAudioInfo> = unsupported()

    override suspend fun exportAudioToPublicStorage(
        sourceAudioFileFullPath: String,
        title: String,
        artist: String?,
        targetList: List<AudioExportTarget>,
    ): Result<ExportedAudioInfo> = unsupported()

    override suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit> = runCatching {
        NSFileManager.defaultManager.removeItemAtPath(fileFullPath, error = null)
    }

    override suspend fun listAudioAndImageFileFullPaths(): Result<List<String>> = runCatching {
        withContext(Dispatchers.IO) {
            val audio = audioDir()
            val image = imageDir()
            fileNamesIn(audio).map { "$audio/$it" } + fileNamesIn(image).map { "$image/$it" }
        }
    }

    override fun clearDiskCache(): Result<Unit> = runCatching { }

    override suspend fun appendAppLog(date: String, line: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val path = logDir() + "/$date.log"
            val data = ("$line\n" as NSString).dataUsingEncoding(NSUTF8StringEncoding)
                ?: error("로그 줄을 인코딩할 수 없습니다.")

            // `NSFileHandle` 은 없는 파일을 만들어 주지 않는다. 첫 줄은 그냥 쓰고 그 뒤로 붙인다.
            val handle = NSFileHandle.fileHandleForWritingAtPath(path)
            if (handle == null) {
                "$line\n".writeToFileAtPath(path)
            } else {
                handle.seekToEndOfFile()
                handle.writeData(data)
                handle.closeFile()
            }

            cleanupOldLogFile()
        }
    }

    override suspend fun fetchAppLogDateList(): Result<List<String>> = runCatching {
        withContext(Dispatchers.IO) { logFileDatesDescending() }
    }

    override suspend fun fetchAppLogLineList(date: String): Result<List<String>> = runCatching {
        withContext(Dispatchers.IO) {
            val text = NSString.stringWithContentsOfFile(
                path = logDir() + "/$date.log",
                encoding = NSUTF8StringEncoding,
                error = null,
            ) ?: return@withContext emptyList()

            // 최신 줄이 위로 와야 한다(Android 와 같은 순서).
            text.lines().filter { it.isNotEmpty() }.asReversed()
        }
    }

    private fun audioDir(): String = directory(AUDIO_DIR_NAME)

    private fun imageDir(): String = directory(IMAGE_DIR_NAME)

    private fun lyricDir(): String = directory(LYRIC_DIR_NAME)

    private fun logDir(): String = directory(LOG_DIR_NAME)

    private fun fileNamesIn(path: String): List<String> =
        NSFileManager.defaultManager.contentsOfDirectoryAtPath(path, error = null)
            ?.filterIsInstance<String>()
            ?.filter { name -> !isDirectory(path + "/" + name) }
            ?: emptyList()

    private fun isDirectory(path: String): Boolean = memScoped {
        val flag = alloc<BooleanVar>()
        NSFileManager.defaultManager.fileExistsAtPath(path, isDirectory = flag.ptr)
        flag.value
    }

    private fun logFileDatesDescending(): List<String> =
        fileNamesIn(logDir())
            .filter { name -> name.endsWith(".log") }
            .map { name -> name.removeSuffix(".log") }
            .sortedDescending()

    private fun cleanupOldLogFile() {
        logFileDatesDescending().drop(MAX_LOG_FILE_COUNT).forEach { date ->
            NSFileManager.defaultManager.removeItemAtPath(logDir() + "/$date.log", error = null)
        }
    }

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

    override suspend fun copyImageFile(
        sourceFileNameWithExt: String,
        targetFileName: String,
    ): Result<String> = unsupported()

    private fun <T> unsupported(): Result<T> = Result.failure(
        UnsupportedOperationException("iOS에서는 이번 범위에서 지원하지 않는 파일 작업입니다.")
    )
}

private fun String.appendPathComponent(component: String): String = trimEnd('/') + "/" + component

@OptIn(ExperimentalForeignApi::class)
private fun String.writeToFileAtPath(path: String) {
    val written = (this as NSString).writeToFile(
        path = path,
        atomically = true,
        encoding = NSUTF8StringEncoding,
        error = null,
    )
    if (!written) error("파일을 저장할 수 없습니다: $path")
}

/**
 * `NSData.create(bytes =)` 는 포인터를 받는다. 빈 배열은 `addressOf(0)` 이 터지므로 따로 막는다.
 */
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
