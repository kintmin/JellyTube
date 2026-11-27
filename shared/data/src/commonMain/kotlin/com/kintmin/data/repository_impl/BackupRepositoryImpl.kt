package com.kintmin.data.repository_impl

import com.kintmin.data.backup.BackupCodec
import com.kintmin.data.backup.BackupFileName
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
import com.kintmin.domain.backup.model.BackupVersionUnsupportedException
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
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BackupRepositoryImpl(
    private val datastoreUtil: DatastoreUtil,
    private val fileManager: FileManager,
    private val backupFolderAccess: BackupFolderAccess,
) : BackupRepository {

    private val fileSystem = FileSystem.SYSTEM

    // 앱이 내려갈 때와 하루 예약 작업이 겹치면 같은 파일에 함께 쓴다.
    private val writeLock = Mutex()

    override fun getStatusFlow(): Flow<BackupStatus> = combine(
        datastoreUtil.getData(PreferencesKey.BackupFolderId),
        datastoreUtil.getData(PreferencesKey.BackupFolderLabel),
        datastoreUtil.getData(PreferencesKey.BackupLastSuccessMillis),
        datastoreUtil.getData(PreferencesKey.BackupLastFailureMillis),
        datastoreUtil.getData(PreferencesKey.BackupLastFailureReason),
    ) { folderId, folderLabel, lastSuccess, lastFailure, failureReason ->
        BackupStatus(
            folder = folderId?.let { BackupFolder(it, folderLabel.orEmpty()) },
            lastSuccessMillis = lastSuccess,
            lastFailureMillis = lastFailure,
            lastFailureReason = failureReason?.let { name -> BackupFailureReason.entries.firstOrNull { it.name == name } },
        )
    }

    override suspend fun registerFolder(folder: BackupFolder): Result<Unit> = suspendRunCatching {
        datastoreUtil.updateData(PreferencesKey.BackupFolderId, folder.id).getOrThrow()
        datastoreUtil.updateData(PreferencesKey.BackupFolderLabel, folder.label).getOrThrow()
    }

    override suspend fun write(content: BackupContent): Result<Long> = writeLock.withLock {
        suspendRunCatching {
            val folderId = folderId() ?: throw BackupFolderNotRegisteredException()
            val installId = getInstallId().getOrThrow()
            val fileName = BackupFileName.build(content.createdAtMillis, TimeZone.currentSystemDefault(), installId)
            val bytes = withContext(Dispatchers.Default) { BackupCodec.encode(content.copy(installId = installId)) }
            backupFolderAccess.write(folderId, fileName, bytes)?.let { refreshed ->
                datastoreUtil.updateData(PreferencesKey.BackupFolderId, refreshed).getOrThrow()
            }
            // 다 쓴 뒤에 치운다. 쓰다 끊기면 어제 것이 남아야 한다.
            suspendRunCatching { deleteOwnCopiesExcept(folderId, installId, keep = fileName) }
            bytes.size.toLong()
        }
    }

    override suspend fun readFolderCopy(): Result<BackupContent?> = suspendRunCatching {
        val folderId = folderId() ?: return@suspendRunCatching null
        val candidates = BackupFileName.candidatesNewestFirst(backupFolderAccess.list(folderId))
        var firstFailure: Throwable? = null
        for (fileName in candidates) {
            val bytes = backupFolderAccess.read(folderId, fileName) ?: continue
            val decoded = suspendRunCatching { withContext(Dispatchers.Default) { BackupCodec.decode(bytes) } }
            decoded.getOrNull()?.let { return@suspendRunCatching it }
            val error = decoded.exceptionOrNull()
            if (error is BackupVersionUnsupportedException) throw error
            if (firstFailure == null) firstFailure = error
        }
        firstFailure?.let { throw it }
        null
    }

    private suspend fun deleteOwnCopiesExcept(folderId: String, installId: String, keep: String) {
        backupFolderAccess.list(folderId)
            .filter { it != keep && BackupFileName.parse(it)?.installId == installId }
            .forEach { backupFolderAccess.delete(folderId, it) }
    }

    override suspend fun getInstallId(): Result<String> = suspendRunCatching {
        withContext(Dispatchers.IO) {
            val path = installIdPath()
            val saved = if (fileSystem.exists(path)) fileSystem.read(path) { readUtf8() }.trim() else ""
            saved.ifEmpty {
                val created = Uuid.random().toString()
                fileSystem.write(path) { writeUtf8(created) }
                created
            }
        }
    }

    override suspend fun adoptInstallId(installId: String): Result<Unit> = suspendRunCatching {
        withContext(Dispatchers.IO) {
            fileSystem.write(installIdPath()) { writeUtf8(installId) }
        }
    }

    override suspend fun recordSuccess(atMillis: Long): Result<Unit> =
        datastoreUtil.updateData(PreferencesKey.BackupLastSuccessMillis, atMillis)

    override suspend fun recordFailure(atMillis: Long, reason: BackupFailureReason): Result<Unit> = suspendRunCatching {
        datastoreUtil.updateData(PreferencesKey.BackupLastFailureReason, reason.name).getOrThrow()
        datastoreUtil.updateData(PreferencesKey.BackupLastFailureMillis, atMillis).getOrThrow()
    }

    private suspend fun folderId(): String? = datastoreUtil.getData(PreferencesKey.BackupFolderId).first()

    private fun installIdPath(): Path =
        fileManager.getInstallIdDirPath().getOrThrow().toPath() / BackupFiles.INSTALL_ID_FILE_NAME
}
