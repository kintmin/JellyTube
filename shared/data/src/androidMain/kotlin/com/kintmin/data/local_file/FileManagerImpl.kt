package com.kintmin.data.local_file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.kintmin.data.local_file.model.CopiedAudioInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import androidx.core.net.toUri

internal class FileManagerImpl(
    private val appContext: Context,
) : FileManager {

    private companion object {

        const val LOG_DIR_NAME = "app_logs"
        const val LYRIC_DIR_NAME = "lyrics"
        const val MAX_LOG_FILE_COUNT = 14
    }

    override fun getFileNameWithExt(fileFullPath: String) = runCatching {
        File(fileFullPath).name
    }

    override fun getAudioDownloadBasePath(fileName: String) = runCatching {
        audioDir().resolve(fileName).absolutePath
    }

    override fun getAudioFileFullPath(fileNameWithExt: String) = runCatching {
        audioDir().resolve(fileNameWithExt).absolutePath
    }

    override fun getImageFileFullPath(fileNameWithExt: String) = runCatching {
        imageDir().resolve(fileNameWithExt).absolutePath
    }

    override fun getLyricFileFullPath(fileNameWithExt: String) = runCatching {
        lyricDir().resolve(fileNameWithExt).absolutePath
    }

    override suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String) =
        runCatching {
            withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }

                val outputFile = imageDir().resolve("$fileName.webp")
                FileOutputStream(outputFile).use { outputStream ->
                    bitmap.compress(format, 60, outputStream)
                }

                "webp"
            }
        }

    override suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val ext = if (synced) "lrc" else "txt"
            val outputFile = lyricDir().resolve("$fileName.$ext")
            outputFile.writeText(text)
            ext
        }
    }

    override suspend fun fetchLyrics(fileNameWithExt: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val file = lyricDir().resolve(fileNameWithExt)
            if (!file.exists()) throw Exception("가사 파일을 찾을 수 없습니다.")
            file.readText()
        }
    }

    override suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo> = runCatching {
        withContext(Dispatchers.IO) {
            val uri = contentUriString.toUri()
            val contentResolver = appContext.contentResolver

            // 파일명과 MIME 타입 조회
            val (displayName, mimeType) = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null
                    name to mime
                } else null to null
            } ?: (null to null)

            val targetExt = resolveAudioExt(mimeType, displayName)
            val fileName = UUID.randomUUID().toString()
            val fileNameWithExt = "$fileName.$targetExt"
            val targetFile = audioDir().resolve(fileNameWithExt)

            // 파일 복사 + SHA-256 해시 계산
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                    }
                }
            } ?: throw Exception("content URI를 열 수 없습니다: $contentUriString")

            val sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }

            // 메타데이터 추출
            val metadata = extractAudioMetadata(targetFile, fileName)

            CopiedAudioInfo(
                fileNameWithExt = fileNameWithExt,
                sha256Hex = sha256Hex,
                title = metadata.title?.takeIf { it.isNotBlank() } ?: displayName?.substringBeforeLast("."),
                artist = metadata.artist?.takeIf { it.isNotBlank() },
                durationMs = metadata.durationMs,
                imageFileNameWithExt = metadata.imageFileNameWithExt,
            )
        }
    }

    override suspend fun saveUploadedAudio(bytes: ByteArray, originalFileName: String): Result<CopiedAudioInfo> = runCatching {
        withContext(Dispatchers.IO) {
            val ext = originalFileName.substringAfterLast(".", "").ifBlank { "mp3" }
            val fileName = UUID.randomUUID().toString()
            val fileNameWithExt = "$fileName.$ext"
            val targetFile = audioDir().resolve(fileNameWithExt)

            val digest = MessageDigest.getInstance("SHA-256")
            FileOutputStream(targetFile).use { output ->
                output.write(bytes)
                digest.update(bytes)
            }
            val sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }

            val metadata = extractAudioMetadata(targetFile, fileName)

            CopiedAudioInfo(
                fileNameWithExt = fileNameWithExt,
                sha256Hex = sha256Hex,
                title = metadata.title?.takeIf { it.isNotBlank() } ?: originalFileName.substringBeforeLast("."),
                artist = metadata.artist?.takeIf { it.isNotBlank() },
                durationMs = metadata.durationMs,
                imageFileNameWithExt = metadata.imageFileNameWithExt,
            )
        }
    }

    private suspend fun extractAudioMetadata(file: File, imageFileName: String): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val imageFileNameWithExt = retriever.embeddedPicture?.let { imageData ->
                saveImageWithCompression(imageData, imageFileName).getOrNull()?.let { ext ->
                    "$imageFileName.$ext"
                }
            }
            AudioMetadata(title, artist, durationMs, imageFileNameWithExt)
        }.getOrElse {
            AudioMetadata()
        }.also {
            retriever.release()
        }
    }

    private data class AudioMetadata(
        val title: String? = null,
        val artist: String? = null,
        val durationMs: Long? = null,
        val imageFileNameWithExt: String? = null,
    )

    /**
     * 외부 공유/파일선택기로 들어온 content URI 오디오를 저장할 실제 확장자를 결정한다.
     *
     * content URI는 임의의 외부 ContentProvider가 만든 것이라 displayName(OpenableColumns.DISPLAY_NAME)에
     * 확장자가 포함된다는 보장이 없다(값 자체가 없거나 "audio"처럼 확장자 없는 이름일 수 있다). 이때 파일을
     * 잘못된 확장자로 저장하면 확장자를 신뢰하는 iOS(AVFoundation)/파일공유 대상에서 재생이 실패한다.
     * 그래서 형식의 authoritative 소스인 MIME 타입을 우선 사용하고, 없으면 displayName의 확장자,
     * 그것도 없으면 mp3로 폴백한다.
     *
     * 주의: 아래 MIME 표는 흔한 타입만 담은 부분 목록이라 모든 코덱을 커버하지 못한다. 표에 없는 MIME은
     * displayName의 확장자로 처리되고, displayName에도 확장자가 없으면 mp3로 폴백된다 — 이 마지막 경우엔
     * 확장자가 틀릴 수 있으므로, 특정 코덱이 실제로 문제되면 그 MIME을 표에 한 줄 추가한다.
     * 목록 참고: https://www.iana.org/assignments/media-types/media-types.xhtml
     *
     * 표의 키는 IANA 정식 등록명이 아니라 Android provider가 현실에서 실제로 내보내는 값 기준이다. 그래서
     * 정식 등록 전 관례였던 x- 변형(audio/x-flac 등, RFC 6648에서 deprecated지만 여전히 통용)도 함께 매칭한다.
     */
    private fun resolveAudioExt(mimeType: String?, displayName: String?): String {
        val fromMime = when (mimeType) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/ogg", "audio/vorbis", "audio/x-ogg" -> "ogg"
            "audio/opus" -> "opus"
            "audio/webm" -> "webm"
            "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a"
            "audio/aac", "audio/x-aac" -> "aac"
            "audio/x-ms-wma", "audio/wma" -> "wma"
            "audio/aiff", "audio/x-aiff" -> "aiff"
            else -> null
        }
        if (fromMime != null) return fromMime

        return displayName?.substringAfterLast(".", "")?.ifBlank { null } ?: "mp3"
    }

    override suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val file = File(fileFullPath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override suspend fun listAudioAndImageFileFullPaths(): Result<List<String>> = runCatching {
        withContext(Dispatchers.IO) {
            val audioFilePaths = audioDir().listFiles()
                ?.filter { it.isFile }
                ?.map { it.absolutePath }
                ?: emptyList()
            val imageFilePaths = imageDir().listFiles()
                ?.filter { it.isFile }
                ?.map { it.absolutePath }
                ?: emptyList()
            audioFilePaths + imageFilePaths
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

    private fun audioDir(): File =
        resolveDirectory(appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC))

    private fun imageDir(): File =
        resolveDirectory(appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES))

    private fun lyricDir(): File =
        resolveDirectory(appContext.filesDir.resolve(LYRIC_DIR_NAME))

    private fun resolveDirectory(dir: File?): File {
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
