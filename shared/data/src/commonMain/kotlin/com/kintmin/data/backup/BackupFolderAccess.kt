package com.kintmin.data.backup

/**
 * 앱을 지워도 남는 저장소. [folderId] 는 사용자가 고른 폴더를 다시 열 수 있는 값이라 플랫폼만 해석하고,
 * 재설치하면 그 값으로도 열 수 없어 사용자가 다시 골라야 한다.
 */
interface BackupFolderAccess {

    /**
     * [folderId] 의 [fileName] 을 [bytes] 로 쓴다. **같은 이름이 이미 있으면 덮어쓰므로 쓰다
     * 끊기면 그 파일이 잘린 채 남는다** — 이름이 다르면 새로 만들어 전 파일은 그대로다.
     * 다시 열 값이 바뀌었으면 새 값을, 아니면 null 을 돌려준다.
     * 폴더에 쓸 수 없으면 `BackupFolderUnavailableException` 을 던진다.
     */
    suspend fun write(folderId: String, fileName: String, bytes: ByteArray): String?

    /** 폴더에 있는 파일 이름 전부. 폴더를 열 수 없으면 `BackupFolderUnavailableException` 을 던진다. */
    suspend fun list(folderId: String): List<String>

    /**
     * 파일이 없으면 null. **폴더를 열 수 없는 것은 없는 것과 다르게 다룬다** — 그때는 던진다.
     * 같게 보면 권한이 풀린 것을 "백업이 없다" 로 읽어 남은 백업을 덮는다.
     */
    suspend fun read(folderId: String, fileName: String): ByteArray?

    /** 없는 파일을 지우는 것은 성공이다. */
    suspend fun delete(folderId: String, fileName: String)
}
