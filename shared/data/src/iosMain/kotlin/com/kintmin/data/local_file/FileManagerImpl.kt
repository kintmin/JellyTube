package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.ExportedAudioInfo
import com.kintmin.domain.audio_media.model.AudioExportTarget
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVMetadataCommonKeyArtist
import platform.AVFoundation.AVMetadataCommonKeyArtwork
import platform.AVFoundation.AVMetadataCommonKeyTitle
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.commonKey
import platform.AVFoundation.commonMetadata
import platform.AVFoundation.dataValue
import platform.AVFoundation.stringValue
import platform.AVFoundation.duration
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
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

        /** 확장자를 모를 때. Android 와 같은 폴백이다. */
        const val DEFAULT_AUDIO_EXT = "mp3"
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

    /**
     * 파일 선택기·외부 공유로 들어온 오디오를 앱 안으로 들인다.
     *
     * 파라미터 이름은 Android SAF 개념이지만 문자열이라 iOS 는 `file://` 주소를 그대로 받는다.
     * 선택기를 `asCopy` 로 열기 때문에 여기 오는 것은 이미 임시 디렉터리 안의 복사본이고,
     * 그래서 보안 스코프(`startAccessingSecurityScopedResource`)를 열 필요가 없다.
     */
    override suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo> = runCatching {
        withContext(Dispatchers.IO) {
            val sourcePath = contentUriString.toLocalFilePath()
                ?: error("파일 주소를 해석할 수 없습니다.")

            val data = NSData.dataWithContentsOfFile(sourcePath)
                ?: error("파일을 읽을 수 없습니다.")

            val ext = sourcePath.substringAfterLast('.', "").lowercase().ifBlank { DEFAULT_AUDIO_EXT }
            val fileName = NSUUID().UUIDString()
            val fileNameWithExt = "$fileName.$ext"

            if (!data.writeToFile(audioDir() + "/$fileNameWithExt", atomically = true)) {
                error("파일을 저장할 수 없습니다.")
            }

            val metadata = readAudioMetadata(audioDir() + "/$fileNameWithExt", fileName)
            CopiedAudioInfo(
                fileNameWithExt = fileNameWithExt,
                sha256Hex = data.sha256Hex(),
                // 파일에 제목이 없으면 **원본 파일명**으로 떨어진다. 저장용 이름(UUID)으로
                // 떨어지면 사용자에게 의미 없는 문자열이 제목으로 보인다(Android 와 같은 선).
                title = metadata.title?.takeIf { it.isNotBlank() }
                    ?: sourcePath.substringAfterLast('/').substringBeforeLast('.'),
                artist = metadata.artist,
                durationMs = metadata.durationMs,
                imageFileNameWithExt = metadata.imageFileNameWithExt,
            )
        }
    }

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

    /**
     * 음원에 박혀 있는 제목·아티스트·길이·앨범아트를 읽는다.
     *
     * Android 의 `MediaMetadataRetriever` 에 대응한다. 실패는 전부 "없음"으로 떨어뜨린다 —
     * 메타데이터가 없다고 들여오기를 막을 이유가 없다.
     */
    private suspend fun readAudioMetadata(fileFullPath: String, imageFileName: String): AudioMetadata {
        val asset = AVURLAsset(uRL = NSURL.fileURLWithPath(fileFullPath), options = null)

        // `CMTimeGetSeconds` 를 쓰지 않는다 — 구조체를 값으로 넘기는 호출이라 Kotlin/Native
        // 에서 길이가 2배로 나왔다(19초 파일이 38초). 필드를 직접 읽는 쪽이 안전하다.
        val durationMs = asset.duration.useContents {
            val scale = timescale.toDouble()
            if (scale <= 0.0) null else value.toDouble() / scale
        }
            ?.takeIf { seconds -> seconds.isFinite() && seconds > 0.0 }
            ?.let { seconds -> (seconds * 1000.0).toLong() }

        var title: String? = null
        var artist: String? = null
        var artwork: NSData? = null

        asset.commonMetadata.filterIsInstance<AVMetadataItem>().forEach { item ->
            when (item.commonKey) {
                AVMetadataCommonKeyTitle -> title = item.stringValue
                AVMetadataCommonKeyArtist -> artist = item.stringValue
                AVMetadataCommonKeyArtwork -> artwork = item.dataValue
            }
        }

        val imageFileNameWithExt = artwork?.toByteArray()?.let { bytes ->
            saveImageWithCompression(bytes, imageFileName).getOrNull()?.let { ext -> "$imageFileName.$ext" }
        }

        return AudioMetadata(title, artist, durationMs, imageFileNameWithExt)
    }

    private data class AudioMetadata(
        val title: String? = null,
        val artist: String? = null,
        val durationMs: Long? = null,
        val imageFileNameWithExt: String? = null,
    )

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

/** `file://` 주소든 그냥 경로든 로컬 파일 경로로 바꾼다. 둘 다 호출부에서 들어온다. */
private fun String.toLocalFilePath(): String? =
    if (startsWith("file://")) NSURL.URLWithString(this)?.path else takeIf { it.startsWith("/") }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.sha256Hex(): String = memScoped {
    val digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
    CC_SHA256(bytes, length.convert(), digest)
    (0 until CC_SHA256_DIGEST_LENGTH).joinToString("") { index ->
        digest[index].toInt().toString(16).padStart(2, '0')
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val source = bytes?.reinterpret<ByteVar>() ?: return ByteArray(0)
    return ByteArray(size) { index -> source[index] }
}

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
