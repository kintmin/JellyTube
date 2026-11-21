package com.kintmin.data.backup

object BackupFiles {
    /** 앱 안 사본을 두는 곳. Android 클라우드 자동 백업은 이 디렉터리만 싣는다. */
    const val LOCAL_DIR_NAME = "backup"
    const val FILE_NAME = "jellytube-backup.json.gz"

    // Android 클라우드 자동 백업의 앱당 한도가 25MB 다. 넘으면 그날 백업이 통째로 빠진다.
    const val LOCAL_COPY_MAX_BYTES = 24L * 1024 * 1024
}
