package com.kintmin.domain.backup.repository

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFailureReason
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.model.BackupWriteResult
import kotlinx.coroutines.flow.Flow

interface BackupRepository {

    fun getStatusFlow(): Flow<BackupStatus>

    suspend fun registerFolder(folder: BackupFolder): Result<Unit>

    /** 고른 폴더와 앱 안 사본을 통째로 바꿔 쓴다. 쓰다 실패하면 전에 있던 백업이 그대로 남는다. */
    suspend fun write(content: BackupContent): Result<BackupWriteResult>

    /** [fileId] 는 플랫폼 파일 선택기가 돌려준 값이다. */
    suspend fun read(fileId: String): Result<BackupContent>

    /** 클라우드 자동 백업으로 되살아났거나 이 기기에서 쓴 사본. 없으면 null. */
    suspend fun readLocalCopy(): Result<BackupContent?>

    /** 백업 폴더의 파일. 폴더가 없거나 이 설치가 그 파일을 볼 수 없으면 null. */
    suspend fun readFolderCopy(): Result<BackupContent?>

    /** [readLocalCopy] 와 달리 풀지 않고 있는지만 본다. */
    suspend fun hasLocalCopy(): Boolean

    suspend fun recordSuccess(atMillis: Long): Result<Unit>

    suspend fun recordFailure(atMillis: Long, reason: BackupFailureReason): Result<Unit>
}
