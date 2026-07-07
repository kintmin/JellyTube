package com.kintmin.data.local_file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.local_file.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

internal class FileManagerImpl constructor(
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

    override suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<Ext> = runCatching {
        withContext(Dispatchers.IO) {
            val ext = if (synced) Ext.LRC else Ext.TXT
            val outputFile = getDirectory(FileType.Lyric).resolve("$fileName.$ext")
            outputFile.writeText(text)
            ext
        }
    }

    override suspend fun fetchLyrics(fileNameWithExt: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val (_, ext) = extractExtFromFileName(fileNameWithExt)
            val file = getDirectory(ext.fileType).resolve(fileNameWithExt)
            if (!file.exists()) throw Exception("가사 파일을 찾을 수 없습니다.")
            file.readText()
        }
    }

    override suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo> = runCatching {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(contentUriString)
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
            val targetFile = getDirectory(FileType.Audio).resolve(fileNameWithExt)

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
            val extName = originalFileName.substringAfterLast(".", "")
            val ext = Ext.entries.find { it.fileType == FileType.Audio && it.name.equals(extName, ignoreCase = true) } ?: Ext.MP3
            val fileName = UUID.randomUUID().toString()
            val fileNameWithExt = "$fileName.$ext"
            val targetFile = getDirectory(FileType.Audio).resolve(fileNameWithExt)

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

    private fun resolveAudioExt(mimeType: String?, displayName: String?): Ext {
        val fromMime = when (mimeType) {
            "audio/mpeg", "audio/mp3" -> Ext.MP3
            "audio/wav", "audio/x-wav", "audio/wave" -> Ext.WAV
            "audio/flac", "audio/x-flac" -> Ext.FLAC
            "audio/ogg", "audio/vorbis", "audio/x-ogg" -> Ext.OGG
            "audio/mp4", "audio/x-m4a", "audio/m4a" -> Ext.M4A
            "audio/aac", "audio/x-aac" -> Ext.AAC
            else -> null
        }
        if (fromMime != null) return fromMime

        val extName = displayName?.substringAfterLast(".", "").orEmpty()
        return Ext.entries.find { it.fileType == FileType.Audio && it.name.equals(extName, ignoreCase = true) }
            ?: Ext.MP3
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

    override suspend fun listAudioAndImageFileNames(): Result<List<String>> = runCatching {
        withContext(Dispatchers.IO) {
            val audioFileNames = getDirectory(FileType.Audio).listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                ?: emptyList()
            val imageFileNames = getDirectory(FileType.Image).listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                ?: emptyList()
            audioFileNames + imageFileNames
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
        return Ext.entries.find { it.name.equals(extName, ignoreCase = true) } ?: throw Exception("올바르지 않은 파일 확장자입니다.")
    }

    private fun getDirectory(fileType: FileType): File {
        val dir = when (fileType) {
            FileType.Audio -> appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            FileType.Image -> appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            FileType.Lyric -> appContext.filesDir.resolve(LYRIC_DIR_NAME)
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
