package com.kintmin.domain.backup.repository

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFailureReason
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupStatus
import kotlinx.coroutines.flow.Flow

interface BackupRepository {

    fun getStatusFlow(): Flow<BackupStatus>

    suspend fun registerFolder(folder: BackupFolder): Result<Unit>

    /** 고른 폴더에 쓰고 쓴 바이트 수를 돌려준다. 쓰다 실패하면 전에 있던 백업이 그대로 남는다. */
    suspend fun write(content: BackupContent): Result<Long>

    /** 폴더에서 되돌릴 백업을 고른다 — **가장 최근 것 하나**다. 어느 설치가 쓴 것인지는 가리지 않는다. */
    suspend fun readFolderCopy(): Result<BackupContent?>

    /** 이 설치를 가리키는 값. 없으면 만들어 둔다. 백업 파일 이름이 이것으로 갈라진다. */
    suspend fun getInstallId(): Result<String>

    suspend fun adoptInstallId(installId: String): Result<Unit>

    suspend fun recordSuccess(atMillis: Long): Result<Unit>

    suspend fun recordFailure(atMillis: Long, reason: BackupFailureReason): Result<Unit>
}
