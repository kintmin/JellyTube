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
            findOwn(fileName)?.let { uri -> resolver.openInputStream(uri)?.use { it.readBytes() } }
        } else {
            legacyFile(fileName).takeIf { it.exists() && hasLegacyPermission() }?.readBytes()
        }
    }

    override suspend fun readPicked(fileId: String): ByteArray = withContext(Dispatchers.IO) {
        resolver.openInputStream(Uri.parse(fileId))?.use { it.readBytes() } ?: throw FileNotFoundException(fileId)
    }

    /** 다 쓴 뒤에 이름을 바꿔 끼운다. 쓰다 끊기면 대기 중인 임시 항목만 남고 전 파일은 그대로다. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToMediaStore(fileName: String, bytes: ByteArray) {
        val tempName = "$fileName.tmp"
        findOwn(tempName)?.let { resolver.delete(it, null, null) }
        val temp = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, tempName)
                put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
                // gzip 으로 주면 제공자가 이름 뒤에 확장자를 또 붙인다.
                put(MediaStore.Downloads.MIME_TYPE, OCTET_STREAM)
                put(MediaStore.Downloads.IS_PENDING, 1)
            },
        ) ?: throw BackupFolderUnavailableException()
        try {
            resolver.openOutputStream(temp, "w")?.use { it.write(bytes) } ?: throw BackupFolderUnavailableException()
            findOwn(fileName)?.let { resolver.delete(it, null, null) }
            resolver.update(
                temp,
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.IS_PENDING, 0)
                },
                null,
                null,
            )
        } catch (e: Throwable) {
            runCatching { resolver.delete(temp, null, null) }
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findOwn(displayName: String): Uri? {
        val columns = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
        resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, columns, selection, arrayOf(displayName, RELATIVE_PATH), null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0).toString())
                }
            }
        return null
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
