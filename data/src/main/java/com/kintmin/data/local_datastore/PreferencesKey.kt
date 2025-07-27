package com.kintmin.data.local_datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

sealed interface PreferencesKey<T> {
    val key: Preferences.Key<T>

    object IsPlaybackRepeating : PreferencesKey<Boolean> {
        override val key = booleanPreferencesKey("IsPlaybackRepeating")
    }

    object IsPlaybackShuffling : PreferencesKey<Boolean> {
        override val key = booleanPreferencesKey("IsPlaybackShuffling")
    }

    object UserId : PreferencesKey<String> {
        override val key = stringPreferencesKey("UserId")
    }
}