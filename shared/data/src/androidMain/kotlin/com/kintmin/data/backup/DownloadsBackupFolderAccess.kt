package com.kintmin.data.backup

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupFolderUnavailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/**
 * 공용 `Download/JellyTube` 에 둔다. 29 부터는 MediaStore 로 권한 없이 쓰지만 **이 설치가 만든 파일만** 보인다 —
 * 재설치한 앱은 예전 파일을 읽지도 덮지도 못해 같은 이름 뒤에 번호가 붙은 새 파일을 만든다.
 */
internal class DownloadsBackupFolderAccess(private val context: Context) : BackupFolderAccess {

    override val fixedFolder = BackupFolder(id = FOLDER_ID, label = "${Environment.DIRECTORY_DOWNLOADS}/$DIR_NAME")

    private val resolver get() = context.contentResolver

    override suspend fun write(folderId: String, fileName: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) writeToMediaStore(fileName, bytes) else writeLegacy(fileName, bytes)
        } catch (e: SecurityException) {
            throw BackupFolderUnavailableException(e)
        }
        null
    }

    override suspend fun read(folderId: String, fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findOwnCopies(fileName).firstOrNull()?.let { uri -> resolver.openInputStream(uri)?.use { it.readBytes() } }
        } else {
            legacyFile(fileName).takeIf { it.exists() && hasLegacyPermission() }?.readBytes()
        }
    }

    override suspend fun readPicked(fileId: String): ByteArray = withContext(Dispatchers.IO) {
        resolver.openInputStream(Uri.parse(fileId))?.use { it.readBytes() } ?: throw FileNotFoundException(fileId)
    }

    /** 새 항목을 다 쓴 뒤에야 이 설치의 이전 사본을 지운다. 쓰다 끊기면 대기 중인 새 항목만 버려지고 이전 사본은 남는다. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToMediaStore(fileName: String, bytes: ByteArray) {
        val previous = findOwnCopies(fileName)
        val created = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
                // gzip 으로 주면 제공자가 이름 뒤에 확장자를 또 붙인다.
                put(MediaStore.Downloads.MIME_TYPE, OCTET_STREAM)
                put(MediaStore.Downloads.IS_PENDING, 1)
            },
        ) ?: throw BackupFolderUnavailableException()
        try {
            resolver.openOutputStream(created, "w")?.use { it.write(bytes) } ?: throw BackupFolderUnavailableException()
            resolver.update(created, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Throwable) {
            runCatching { resolver.delete(created, null, null) }
            throw e
        }
        previous.forEach { runCatching { resolver.delete(it, null, null) } }
    }

    /** 이 설치가 만든 백업 사본, 최근 것부터. 이름에 번호가 붙었을 수 있어 이름 하나로 찾지 않는다. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findOwnCopies(fileName: String): List<Uri> {
        val columns = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val order = "${MediaStore.Downloads._ID} DESC"
        val copies = mutableListOf<Uri>()
        resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, columns, selection, arrayOf(RELATIVE_PATH), order)
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (isBackupCopyName(cursor.getString(1), fileName)) {
                        copies += Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0).toString())
                    }
                }
            }
        return copies
    }

    @Suppress("DEPRECATION")
    private fun legacyFile(fileName: String): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).resolve(DIR_NAME).resolve(fileName)

    private fun writeLegacy(fileName: String, bytes: ByteArray) {
        if (!hasLegacyPermission()) throw BackupFolderUnavailableException()
        val target = legacyFile(fileName)
        target.parentFile?.mkdirs()
        val temp = File(target.path + ".tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            temp.delete()
            throw BackupFolderUnavailableException()
        }
    }

    private fun hasLegacyPermission() =
        context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val FOLDER_ID = "downloads"
        const val DIR_NAME = "JellyTube"
        val RELATIVE_PATH = "${Environment.DIRECTORY_DOWNLOADS}/$DIR_NAME/"
        const val OCTET_STREAM = "application/octet-stream"
    }
}

/**
 * 같은 이름이 이미 있으면 MediaStore 가 ` (번호)` 를 붙인다. MIME 이 octet-stream 이면 확장자를 몰라 이름 맨 뒤에
 * 붙는다(`jellytube-backup.json.gz (1)`). 확장자 앞에 붙는 제공자도 있어 둘 다 백업 사본으로 본다.
 */
internal fun isBackupCopyName(displayName: String, fileName: String): Boolean {
    if (displayName == fileName) return true
    val stem = fileName.substringBefore('.')
    val extension = fileName.substring(stem.length)
    return numberedSuffix(displayName, "$fileName (", ")") || numberedSuffix(displayName, "$stem (", ")$extension")
}

private fun numberedSuffix(displayName: String, prefix: String, suffix: String): Boolean {
    if (!displayName.startsWith(prefix) || !displayName.endsWith(suffix) || displayName.length < prefix.length + suffix.length) return false
    val number = displayName.substring(prefix.length, displayName.length - suffix.length)
    return number.isNotEmpty() && number.all(Char::isDigit)
}
