package com.kintmin.data.backup

import com.kintmin.data.local_file.toByteArray
import com.kintmin.data.local_file.toNSData
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupFolderUnavailableException
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSData
import platform.Foundation.NSDataWritingAtomic
import platform.Foundation.NSError
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSFileCoordinatorWritingForReplacing
import platform.Foundation.NSFileNoSuchFileError
import platform.Foundation.NSFileReadNoSuchFileError
import platform.Foundation.NSURL
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL

/**
 * 앱을 지워도 남는 공용 폴더가 없어 사용자가 고른 폴더를 쓴다. [BackupFolder.id] 는 그 폴더의
 * security-scoped bookmark(base64)다. 재설치하면 bookmark 도 사라지므로 폴더를 다시 골라야 읽을 수 있다.
 */
@OptIn(ExperimentalForeignApi::class)
internal class BookmarkBackupFolderAccess : BackupFolderAccess {

    override val fixedFolder: BackupFolder? = null

    override suspend fun write(folderId: String, fileName: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        withFolder(folderId) { folder, isStale ->
            val target = folder.URLByAppendingPathComponent(fileName) ?: throw BackupFolderUnavailableException()
            writeCoordinated(target, bytes.toNSData())
            if (isStale) {
                folder.bookmarkDataWithOptions(0u, includingResourceValuesForKeys = null, relativeToURL = null, error = null)
                    ?.base64EncodedStringWithOptions(0u)
            } else {
                null
            }
        }
    }

    override suspend fun read(folderId: String, fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        withFolder(folderId) { folder, _ ->
            val target = folder.URLByAppendingPathComponent(fileName) ?: return@withFolder null
            // 내려받지 않은 iCloud 파일은 경로에 없는 것으로 보인다. 존재를 먼저 묻지 않고 조율 읽기에 내려받기를 맡긴다.
            readCoordinated(target)
        }
    }

    override suspend fun readPicked(fileId: String): ByteArray = withContext(Dispatchers.IO) {
        FileSystem.SYSTEM.read(fileId.toPath()) { readByteArray() }
    }

    private inline fun <T> withFolder(folderId: String, block: (folder: NSURL, isStale: Boolean) -> T): T = memScoped {
        val bookmark = NSData.create(base64EncodedString = folderId, options = 0u) ?: throw BackupFolderUnavailableException()
        val stale = alloc<BooleanVar>()
        val folder = NSURL.URLByResolvingBookmarkData(
            bookmarkData = bookmark,
            options = 0u,
            relativeToURL = null,
            bookmarkDataIsStale = stale.ptr,
            error = null,
        ) ?: throw BackupFolderUnavailableException()
        if (!folder.startAccessingSecurityScopedResource()) throw BackupFolderUnavailableException()
        try {
            block(folder, stale.value)
        } finally {
            folder.stopAccessingSecurityScopedResource()
        }
    }

    /** 다른 앱의 파일 제공자(iCloud Drive 등)가 가진 폴더라 조율해서 쓴다. 원자적으로 써서 실패하면 전 파일이 남는다. */
    private fun writeCoordinated(target: NSURL, data: NSData) = memScoped {
        val coordinatorError = alloc<ObjCObjectVar<NSError?>>()
        var written = false
        var writeError: NSError? = null
        NSFileCoordinator(filePresenter = null).coordinateWritingItemAtURL(
            url = target,
            options = NSFileCoordinatorWritingForReplacing,
            error = coordinatorError.ptr,
        ) { url ->
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                written = url != null && data.writeToURL(url, NSDataWritingAtomic, error.ptr)
                writeError = error.value
            }
        }
        if (!written) {
            val error = coordinatorError.value ?: writeError
            throw IllegalStateException(error?.localizedDescription ?: "백업 파일을 쓰지 못했습니다.")
        }
    }

    /** iCloud Drive 에만 있는 파일이면 조율하는 동안 내려받는다. 파일이 정말 없으면 null. */
    private fun readCoordinated(target: NSURL): ByteArray? = memScoped {
        val coordinatorError = alloc<ObjCObjectVar<NSError?>>()
        var data: NSData? = null
        NSFileCoordinator(filePresenter = null).coordinateReadingItemAtURL(
            url = target,
            options = 0u,
            error = coordinatorError.ptr,
        ) { url -> data = url?.let { NSData.dataWithContentsOfURL(it) } }
        data?.toByteArray() ?: run {
            val error = coordinatorError.value
            if (error == null || error.isNoSuchFile()) return@memScoped null
            throw IllegalStateException(error.localizedDescription)
        }
    }

    private fun NSError.isNoSuchFile() = domain == NSCocoaErrorDomain && (code == NSFileNoSuchFileError || code == NSFileReadNoSuchFileError)
}
