package com.kintmin.domain.backup.model

enum class BackupOutcome {
    WRITTEN,
    /** 음원이 하나도 없으면 쓰지 않는다. 재설치 직후의 빈 상태가 남아 있던 백업을 덮지 않게 한다. */
    SKIPPED_EMPTY,

    /** 고른 폴더에 이미 백업이 있고 이 설치는 아직 백업한 적이 없다. 복원할 수 있게 덮지 않았다. */
    KEPT_EXISTING,
}
