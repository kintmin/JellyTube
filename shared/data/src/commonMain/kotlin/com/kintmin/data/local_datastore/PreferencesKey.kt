package com.kintmin.data.local_datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

sealed interface PreferencesKey<T> {
    val key: Preferences.Key<T>

    data object IsPlaybackRepeating : PreferencesKey<Boolean> {
        override val key = booleanPreferencesKey("IsPlaybackRepeating")
    }

    data object IsPlaybackShuffling : PreferencesKey<Boolean> {
        override val key = booleanPreferencesKey("IsPlaybackShuffling")
    }

    data object PlaybackSpeed : PreferencesKey<Float> {
        override val key = floatPreferencesKey("PlaybackSpeed")
    }

    data object PlaybackPitchSemitone : PreferencesKey<Int> {
        override val key = intPreferencesKey("PlaybackPitchSemitone")
    }

    data object SleepTimerMinutes : PreferencesKey<Int> {
        override val key = intPreferencesKey("SleepTimerMinutes")
    }

    data object SleepTimerAnnouncementMediaId : PreferencesKey<Int> {
        override val key = intPreferencesKey("SleepTimerAnnouncementMediaId")

        /** 저장소가 값을 지우지 못해 "고르지 않음" 을 이 값으로 적는다. 음원 id 는 1 부터 받는다. */
        const val NONE = 0
    }

    data object SecretModeMinutes : PreferencesKey<Int> {
        override val key = intPreferencesKey("SecretModeMinutes")
    }

    data object UserId : PreferencesKey<String> {
        override val key = stringPreferencesKey("UserId")
    }

    data object ShouldInsertAtTopOnDownload : PreferencesKey<Boolean> {
        override val key = booleanPreferencesKey("shouldInsertAtTopOnDownload")
    }

    data object PlaylistIdOnDownload : PreferencesKey<Int> {
        override val key = intPreferencesKey("playlistIdOnDownload")
    }

    data object LastStepSensor : PreferencesKey<Long> {
        override val key = longPreferencesKey("lastStepSensor")
    }

    data object LastStepSensorDate : PreferencesKey<String> {
        override val key = stringPreferencesKey("lastStepSensorDate")
    }

    data object AccelerateStep : PreferencesKey<Int> {
        override val key = intPreferencesKey("accelerateStep")
    }

    data object IsStepEnabled : PreferencesKey<Boolean> {
        override val key = booleanPreferencesKey("isStepEnabled")
    }

    data object BackupFolderId : PreferencesKey<String> {
        override val key = stringPreferencesKey("backupFolderId")
    }

    data object BackupFolderLabel : PreferencesKey<String> {
        override val key = stringPreferencesKey("backupFolderLabel")
    }

    data object BackupLastSuccessMillis : PreferencesKey<Long> {
        override val key = longPreferencesKey("backupLastSuccessMillis")
    }

    data object BackupLastFailureMillis : PreferencesKey<Long> {
        override val key = longPreferencesKey("backupLastFailureMillis")
    }

    /** `BackupFailureReason` 의 name. */
    data object BackupLastFailureReason : PreferencesKey<String> {
        override val key = stringPreferencesKey("backupLastFailureReason")
    }

    /** 첫 화면 안내는 사용자가 답하면 다시 띄우지 않는다. 그 뒤로는 설정에서만 다룬다. */
    data object HasShownNotificationNotice : PreferencesKey<Boolean> {
        override val key = booleanPreferencesKey("hasShownNotificationNotice")
    }

    data object HasShownBackupNotice : PreferencesKey<Boolean> {
        override val key = booleanPreferencesKey("hasShownBackupNotice")
    }

    data object AppUpdateLastCheckMillis : PreferencesKey<Long> {
        override val key = longPreferencesKey("appUpdateLastCheckMillis")
    }

    data object AppUpdateIgnoredVersionName : PreferencesKey<String> {
        override val key = stringPreferencesKey("appUpdateIgnoredVersionName")
    }
}
