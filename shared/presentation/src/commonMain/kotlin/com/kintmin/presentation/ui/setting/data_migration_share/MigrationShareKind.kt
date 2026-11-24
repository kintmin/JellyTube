package com.kintmin.presentation.ui.setting.data_migration_share

import kotlinx.serialization.Serializable

/** 보내기 화면이 라우트로 받는 범위의 종류. 고른 id 는 라우트가 따로 나른다. */
@Serializable
enum class MigrationShareKind {
    SYNC,
    PLAYLIST,
    AUDIO_MEDIA,
}
