package com.kintmin.data.backup

object BackupFiles {
    /** 설치마다 가르기 전에 쓰던 이름. 읽기만 한다 — 누가 쓴 것인지 알 수 없어 덮거나 지우지 않는다. */
    const val LEGACY_FILE_NAME = "jellytube-backup.json.gz"

    /** 설치마다 다른 값을 두는 곳. Android 기기 간 이전 규칙이 이 디렉터리만 뺀다. */
    const val INSTALL_ID_DIR_NAME = "install_id"
    const val INSTALL_ID_FILE_NAME = "install-id"
}
