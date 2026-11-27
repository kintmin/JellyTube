package com.kintmin.domain.backup.model

enum class BackupOutcome {
    WRITTEN,

    /** 음원이 하나도 없으면 쓰지 않는다. 재설치 직후의 빈 상태가 남아 있던 백업을 덮지 않게 한다. */
    SKIPPED_EMPTY,

    /** 고른 폴더에 되돌릴 백업이 있다. 오늘 날짜로 새로 쓰면 그것이 후보에서 밀려 쓰지 않았다. */
    KEPT_EXISTING,

    /** 고른 폴더의 백업을 읽지 못했다. 되돌릴 것일 수 있어 쓰지 않았다. */
    KEPT_UNREADABLE,
}
