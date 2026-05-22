package com.kintmin.data.local_datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
}
