package com.kintmin.data.local_file

import android.content.ContentValues
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
import com.kintmin.data.local_file.model.ExportedAudioInfo
import com.kintmin.domain.audio_media.model.AudioExportTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
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

        /** 공용 저장소의 각 미디어 디렉토리 안에서 이 앱이 쓰는 하위 디렉토리명. */
        const val EXPORT_DIR_NAME = "JellyTube"

        /**
         * 내보낼 파일명 base의 최대 바이트.
         *
         * ext4/FAT의 파일명 상한은 글자수가 아니라 255바이트다. 한글 한 글자가 UTF-8 3바이트라
         * 글자수로 제한하면 상한을 못 막는다. 확장자와 동명이곡 접미사 여유를 두고 150으로 잡는다.
         */
        const val MAX_EXPORT_FILE_NAME_BYTES = 150

        /** 동명이곡 접미사 " (n)" 탐색 상한. */
        const val MAX_EXPORT_NAME_CONFLICT_RETRY = 99

        /** FAT/exFAT과 MediaProvider가 파일명에서 거부하는 문자 + 제어문자. */
        val INVALID_FILE_NAME_CHAR_REGEX = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
        val WHITESPACE_REGEX = Regex("\\s+")
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

    override suspend fun saveUploadedAudio(sourceFileFullPath: String, originalFileName: String): Result<CopiedAudioInfo> = runCatching {
        withContext(Dispatchers.IO) {
            val ext = originalFileName.substringAfterLast(".", "").ifBlank { "mp3" }
            val fileName = UUID.randomUUID().toString()
            val fileNameWithExt = "$fileName.$ext"
            val targetFile = audioDir().resolve(fileNameWithExt)

            // 파일 복사 + SHA-256 해시 계산 (대용량 파일도 메모리에 올리지 않는다)
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(File(sourceFileFullPath)).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                    }
                }
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

    override suspend fun exportAudioToPublicStorage(
        sourceAudioFileFullPath: String,
        title: String,
        artist: String?,
        targetList: List<AudioExportTarget>,
    ): Result<ExportedAudioInfo> = runCatching {
        withContext(Dispatchers.IO) {
            val distinctTargetList = targetList.distinct()
            if (distinctTargetList.isEmpty()) throw Exception("내보낼 용도를 하나 이상 선택해야 합니다.")

            val sourceFile = File(sourceAudioFileFullPath)
            if (!sourceFile.isFile) throw Exception("내보낼 음원 파일을 찾을 수 없습니다.")

            val ext = sourceFile.extension.ifBlank { "mp3" }
            val mimeType = resolveExportMimeType(ext)
            val sourceSize = sourceFile.length()
            val fileNameBase = buildExportFileNameBase(title, artist)

            // 파일명은 첫 대상 기준으로 한 번만 확정해 모든 대상에 같은 이름을 쓴다.
            // 용도별로 이름이 갈리면 사용자가 목록마다 다른 이름을 보게 된다.
            val displayNameWithExt = resolveExportFileName(
                dirType = exportDirType(distinctTargetList.first()),
                fileNameBase = fileNameBase,
                ext = ext,
                sourceSize = sourceSize,
            )

            var exportedAny = false
            distinctTargetList.forEach { target ->
                val dirType = exportDirType(target)
                // 용도별로 따로 검사해서, 일부만 지워진 상태에서도 빠진 것만 채운다.
                if (findExportedFileSize(dirType, displayNameWithExt) == sourceSize) return@forEach

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    insertExportedAudio(sourceFile, dirType, displayNameWithExt, mimeType)
                } else {
                    insertExportedAudioLegacy(sourceFile, dirType, displayNameWithExt, mimeType, target)
                }
                exportedAny = true
            }

            ExportedAudioInfo(
                displayNameWithExt = displayNameWithExt,
                alreadyExported = !exportedAny,
            )
        }
    }

    private fun exportDirType(target: AudioExportTarget): String = when (target) {
        AudioExportTarget.MUSIC -> Environment.DIRECTORY_MUSIC
        AudioExportTarget.ALARM -> Environment.DIRECTORY_ALARMS
        AudioExportTarget.NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
        AudioExportTarget.RINGTONE -> Environment.DIRECTORY_RINGTONES
    }

    /** MediaStore RELATIVE_PATH는 끝 슬래시를 포함한 형태로 저장되므로 조회 인자도 같은 형태여야 한다. */
    private fun exportRelativePath(dirType: String): String = "$dirType/$EXPORT_DIR_NAME/"

    private fun exportCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    /**
     * API 29+ 경로. IS_PENDING으로 항목을 선점하고 스트림에 쓴 뒤 publish 한다.
     *
     * TITLE/ARTIST/DURATION과 IS_MUSIC/IS_ALARM/IS_NOTIFICATION/IS_RINGTONE은 모두 readOnly
     * 컬럼이라 넣어도 MediaProvider가 버린다. 앞의 셋은 파일 태그에서, 뒤의 넷은 파일이 놓인
     * 경로에서 재계산되므로 여기서 제어할 수 있는 것은 DISPLAY_NAME과 RELATIVE_PATH뿐이다.
     * IS_PENDING을 0으로 내리는 시점에 그 스캔이 돌아서 MediaScannerConnection 호출도 필요 없다.
     */
    private fun insertExportedAudio(
        sourceFile: File,
        dirType: String,
        displayNameWithExt: String,
        mimeType: String?,
    ) {
        val contentResolver = appContext.contentResolver
        val pendingValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayNameWithExt)
            put(MediaStore.Audio.Media.RELATIVE_PATH, exportRelativePath(dirType))
            mimeType?.let { put(MediaStore.Audio.Media.MIME_TYPE, it) }
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val itemUri = contentResolver.insert(exportCollection(), pendingValues)
            ?: throw Exception("공용 저장소에 항목을 만들 수 없습니다: ${exportRelativePath(dirType)}$displayNameWithExt")

        runCatching {
            contentResolver.openOutputStream(itemUri, "w")?.use { output ->
                FileInputStream(sourceFile).use { input -> input.copyTo(output) }
            } ?: throw Exception("공용 저장소 파일을 열 수 없습니다.")
        }.onFailure { throwable ->
            // IS_PENDING으로 남은 항목은 사용자에게 0바이트 파일로 보이고 다음 중복 판정도 망친다.
            runCatching { contentResolver.delete(itemUri, null, null) }
            throw throwable
        }

        contentResolver.update(
            itemUri,
            ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
            null,
            null,
        )
    }

    /**
     * API 28 이하 경로. RELATIVE_PATH/IS_PENDING이 없으므로 공용 디렉토리에 직접 쓰고 DATA로 등록한다.
     * WRITE_EXTERNAL_STORAGE 런타임 권한이 필요하다(manifest에 maxSdkVersion="28"로 선언돼 있다).
     */
    @Suppress("DEPRECATION")
    private fun insertExportedAudioLegacy(
        sourceFile: File,
        dirType: String,
        displayNameWithExt: String,
        mimeType: String?,
        target: AudioExportTarget,
    ) {
        val targetDir = Environment.getExternalStoragePublicDirectory(dirType).resolve(EXPORT_DIR_NAME)
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw Exception("공용 저장소 디렉토리를 만들 수 없습니다: ${targetDir.absolutePath}")
        }
        val targetFile = targetDir.resolve(displayNameWithExt)

        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output -> input.copyTo(output) }
        }

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DATA, targetFile.absolutePath)
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayNameWithExt)
            mimeType?.let { put(MediaStore.Audio.Media.MIME_TYPE, it) }
            // 28 이하에선 이 컬럼들이 실제로 먹는다. 29+와 동작을 맞추기 위해 복사한 디렉토리에
            // 대응하는 플래그만 1로 넣는다 — 전부 1로 넣으면 음악 앱에 같은 곡이 여러 번 뜬다.
            put(MediaStore.Audio.Media.IS_MUSIC, if (target == AudioExportTarget.MUSIC) 1 else 0)
            put(MediaStore.Audio.Media.IS_ALARM, if (target == AudioExportTarget.ALARM) 1 else 0)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, if (target == AudioExportTarget.NOTIFICATION) 1 else 0)
            put(MediaStore.Audio.Media.IS_RINGTONE, if (target == AudioExportTarget.RINGTONE) 1 else 0)
        }

        appContext.contentResolver.insert(exportCollection(), values)
            ?: throw Exception("MediaStore에 항목을 등록할 수 없습니다: ${targetFile.absolutePath}")
    }

    /**
     * 공용 저장소에 쓸 파일명 base를 만든다.
     *
     * 디스크 파일명이 UUID라 쓸 수 없고, MediaStore TITLE이 readOnly라서 파일 태그가 없으면
     * 목록 표시명이 파일명에서 파생된다. 즉 알람음/벨소리 목록에 보일 이름을 여기서 결정한다.
     */
    private fun buildExportFileNameBase(title: String, artist: String?): String {
        val raw = listOfNotNull(
            artist?.trim()?.takeIf { it.isNotBlank() },
            title.trim().takeIf { it.isNotBlank() },
        ).joinToString(" - ")

        return raw
            .replace(INVALID_FILE_NAME_CHAR_REGEX, "_")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
            .takeUtf8Bytes(MAX_EXPORT_FILE_NAME_BYTES)
            .trim()
            .trimEnd('.') // 끝 점은 일부 파일시스템과 탐색기에서 잘린다
            .ifBlank { EXPORT_DIR_NAME } // 제목·아티스트가 모두 비어있는 비정상 데이터 방어
    }

    /** UTF-8 바이트 기준 절단. 멀티바이트 문자 중간에서 자르지 않도록 연속 바이트 경계를 뒤로 물린다. */
    private fun String.takeUtf8Bytes(maxBytes: Int): String {
        val bytes = toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) return this

        var end = maxBytes
        while (end > 0 && (bytes[end].toInt() and 0xC0) == 0x80) end--
        return String(bytes, 0, end, Charsets.UTF_8)
    }

    /**
     * 내보낼 파일명을 확정한다.
     *
     * 기기 저장 여부를 DB에 기록하지 않으므로 MediaStore 자체를 진실의 원천으로 쓴다.
     * - 같은 이름 + 같은 크기: 이미 내보낸 음원 → 그 이름을 재사용(복사는 호출부에서 건너뛴다)
     * - 같은 이름 + 다른 크기: 동명이곡 → " (1)", " (2)" ... 로 빈 이름을 찾는다
     *
     * 사용자가 파일 관리자로 지우면 레코드도 사라져 자동으로 다시 내보내진다.
     */
    private fun resolveExportFileName(
        dirType: String,
        fileNameBase: String,
        ext: String,
        sourceSize: Long,
    ): String {
        val candidate = "$fileNameBase.$ext"
        val existingSize = findExportedFileSize(dirType, candidate)
        if (existingSize == null || existingSize == sourceSize) return candidate

        for (index in 1..MAX_EXPORT_NAME_CONFLICT_RETRY) {
            val next = "$fileNameBase ($index).$ext"
            val nextSize = findExportedFileSize(dirType, next)
            if (nextSize == null || nextSize == sourceSize) return next
        }
        throw Exception("같은 이름의 파일이 너무 많습니다: $candidate")
    }

    /**
     * 대상 디렉토리에 같은 이름의 항목이 있으면 그 크기를, 없으면 null을 돌려준다.
     *
     * API 29+에서는 미디어 읽기 권한이 없으면 이 앱이 만든 레코드만 보인다. 앱을 재설치하면
     * 소유권이 사라져 기존 파일이 안 보이고 " (1)" 사본이 생길 수 있다 — 저장 이력을 DB에
     * 남기지 않기로 한 선택의 대가이고, 기능상 치명적이지 않아 그대로 둔다.
     */
    private fun findExportedFileSize(dirType: String, displayNameWithExt: String): Long? {
        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Audio.Media.RELATIVE_PATH}=? AND ${MediaStore.Audio.Media.DISPLAY_NAME}=?"
            selectionArgs = arrayOf(exportRelativePath(dirType), displayNameWithExt)
        } else {
            @Suppress("DEPRECATION")
            val fullPath = Environment.getExternalStoragePublicDirectory(dirType)
                .resolve(EXPORT_DIR_NAME)
                .resolve(displayNameWithExt)
                .absolutePath
            selection = "${MediaStore.Audio.Media.DATA}=?"
            selectionArgs = arrayOf(fullPath)
        }

        return appContext.contentResolver.query(
            exportCollection(),
            arrayOf(MediaStore.Audio.Media.SIZE),
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    /**
     * 공용 저장소 내보내기에 쓸 MIME 타입. [resolveAudioExt] 표의 역방향이다.
     *
     * 표에 없는 확장자는 null을 반환해 MIME_TYPE 컬럼을 생략한다 — 틀린 MIME을 박으면 확장자를
     * 신뢰하는 재생기에서 문제가 되는데, 생략하면 MediaProvider가 파일명 확장자로 알아서 채운다.
     */
    private fun resolveExportMimeType(ext: String): String? = when (ext.lowercase()) {
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        "webm" -> "audio/webm"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "wma" -> "audio/x-ms-wma"
        "aiff" -> "audio/aiff"
        else -> null
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
