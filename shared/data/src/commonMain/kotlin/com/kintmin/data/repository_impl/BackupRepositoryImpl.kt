package com.kintmin.data.repository_impl

import com.kintmin.data.backup.BackupCodec
import com.kintmin.data.backup.BackupFiles
import com.kintmin.data.backup.BackupFolderAccess
import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFailureReason
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupFolderNotRegisteredException
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.model.BackupWriteResult
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.common.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class BackupRepositoryImpl(
    private val datastoreUtil: DatastoreUtil,
    private val fileManager: FileManager,
    private val backupFolderAccess: BackupFolderAccess,
) : BackupRepository {

    private val fileSystem = FileSystem.SYSTEM

    // 앱이 내려갈 때와 하루 예약 작업이 겹치면 같은 임시 파일에 함께 쓴다.
    private val writeLock = Mutex()

    override fun getStatusFlow(): Flow<BackupStatus> = combine(
        datastoreUtil.getData(PreferencesKey.BackupFolderId),
        datastoreUtil.getData(PreferencesKey.BackupFolderLabel),
        datastoreUtil.getData(PreferencesKey.BackupLastSuccessMillis),
        datastoreUtil.getData(PreferencesKey.BackupLastFailureMillis),
        datastoreUtil.getData(PreferencesKey.BackupLastFailureReason),
    ) { folderId, folderLabel, lastSuccess, lastFailure, failureReason ->
        BackupStatus(
            folder = backupFolderAccess.fixedFolder ?: folderId?.let { BackupFolder(it, folderLabel.orEmpty()) },
            lastSuccessMillis = lastSuccess,
            lastFailureMillis = lastFailure,
            lastFailureReason = failureReason?.let { name -> BackupFailureReason.entries.firstOrNull { it.name == name } },
            isFolderFixed = backupFolderAccess.fixedFolder != null,
        )
    }

    override suspend fun registerFolder(folder: BackupFolder): Result<Unit> = suspendRunCatching {
        datastoreUtil.updateData(PreferencesKey.BackupFolderId, folder.id).getOrThrow()
        datastoreUtil.updateData(PreferencesKey.BackupFolderLabel, folder.label).getOrThrow()
    }

    override suspend fun write(content: BackupContent): Result<BackupWriteResult> = writeLock.withLock {
        suspendRunCatching {
            val folderId = folderId() ?: throw BackupFolderNotRegisteredException()
            val bytes = withContext(Dispatchers.Default) { BackupCodec.encode(content) }
            val localCopySkipped = bytes.size > BackupFiles.LOCAL_COPY_MAX_BYTES
            withContext(Dispatchers.IO) {
                if (localCopySkipped) {
                    // 한도를 넘는 사본이 남아 있으면 그것 때문에 클라우드 백업이 통째로 빠진다.
                    fileSystem.delete(localCopyPath(), mustExist = false)
                } else {
                    writeLocalCopy(bytes)
                }
            }
            backupFolderAccess.write(folderId, BackupFiles.FILE_NAME, bytes)?.let { refreshed ->
                datastoreUtil.updateData(PreferencesKey.BackupFolderId, refreshed).getOrThrow()
            }
            BackupWriteResult(sizeBytes = bytes.size.toLong(), localCopySkipped = localCopySkipped)
        }
    }

    override suspend fun read(fileId: String): Result<BackupContent> = suspendRunCatching {
        val bytes = backupFolderAccess.readPicked(fileId)
        withContext(Dispatchers.Default) { BackupCodec.decode(bytes) }
    }

    override suspend fun readLocalCopy(): Result<BackupContent?> = suspendRunCatching {
        val path = localCopyPath()
        val bytes = withContext(Dispatchers.IO) {
            if (fileSystem.exists(path)) fileSystem.read(path) { readByteArray() } else null
        } ?: return@suspendRunCatching null
        withContext(Dispatchers.Default) { BackupCodec.decode(bytes) }
    }

    override suspend fun readFolderCopy(): Result<BackupContent?> = suspendRunCatching {
        val folderId = folderId() ?: return@suspendRunCatching null
        val bytes = backupFolderAccess.read(folderId, BackupFiles.FILE_NAME) ?: return@suspendRunCatching null
        withContext(Dispatchers.Default) { BackupCodec.decode(bytes) }
    }

    override suspend fun hasLocalCopy(): Boolean = withContext(Dispatchers.IO) {
        suspendRunCatching { fileSystem.exists(localCopyPath()) }.getOrDefault(false)
    }

    override suspend fun recordSuccess(atMillis: Long): Result<Unit> =
        datastoreUtil.updateData(PreferencesKey.BackupLastSuccessMillis, atMillis)

    override suspend fun recordFailure(atMillis: Long, reason: BackupFailureReason): Result<Unit> = suspendRunCatching {
        datastoreUtil.updateData(PreferencesKey.BackupLastFailureReason, reason.name).getOrThrow()
        datastoreUtil.updateData(PreferencesKey.BackupLastFailureMillis, atMillis).getOrThrow()
    }

    private fun writeLocalCopy(bytes: ByteArray) {
        val path = localCopyPath()
        val part = "$path.part".toPath()
        fileSystem.write(part) { write(bytes) }
        fileSystem.atomicMove(part, path)
    }

    private suspend fun folderId(): String? =
        backupFolderAccess.fixedFolder?.id ?: datastoreUtil.getData(PreferencesKey.BackupFolderId).first()

    private fun localCopyPath(): Path = fileManager.getBackupDirPath().getOrThrow().toPath() / BackupFiles.FILE_NAME
}
