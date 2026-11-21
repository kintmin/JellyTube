package com.kintmin.domain.backup.model

/** [id] 는 플랫폼이 다시 열 때 쓰는 값이라 해석하지 않는다. [label] 은 보여 주기만 한다. */
data class BackupFolder(
    val id: String,
    val label: String,
)
