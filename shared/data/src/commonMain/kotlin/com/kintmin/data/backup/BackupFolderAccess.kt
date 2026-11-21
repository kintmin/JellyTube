package com.kintmin.data.backup

import com.kintmin.domain.backup.model.BackupFolder

/** 앱을 지워도 남는 저장소. [folderId]·[fileId] 는 플랫폼이 만든 값이라 플랫폼만 해석한다. */
interface BackupFolderAccess {

    /** null 이 아니면 플랫폼이 자리를 정해 두어 사용자가 폴더를 고르지 않는다. */
    val fixedFolder: BackupFolder?

    /**
     * [folderId] 의 [fileName] 을 [bytes] 로 통째로 바꾼다. 쓰다 실패하면 전 파일이 남는다.
     * 다시 열 값이 바뀌었으면 새 값을, 아니면 null 을 돌려준다.
     * 폴더에 쓸 수 없으면 `BackupFolderUnavailableException` 을 던진다.
     */
    suspend fun write(folderId: String, fileName: String, bytes: ByteArray): String?

    /** 파일이 없거나 이 설치가 볼 수 없는 파일이면 null. */
    suspend fun read(folderId: String, fileName: String): ByteArray?

    /** 사용자가 파일 선택기로 고른 파일. */
    suspend fun readPicked(fileId: String): ByteArray
}
