package com.kintmin.data.local_datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kintmin.data.local_datastore.preference_key.BooleanPreferenceKey
import com.kintmin.data.local_datastore.preference_key.StringPreferenceKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DatastoreUtil(
    val context: Context,
) {
    private companion object {
        const val DATASTORE_NAME = "settings"
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

    suspend fun updateBooleanData(key: BooleanPreferenceKey, newData: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                context.dataStore.edit { settings ->
                    settings[booleanPreferencesKey(key.name)] = newData
                }
            }
        }
    }

    suspend fun updateStringData(key: StringPreferenceKey, newData: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                context.dataStore.edit { settings ->
                    settings[stringPreferencesKey(key.name)] = newData
                }
            }
        }
    }

    val isPlaybackRepeatingFlow = context.dataStore.data.map {
        it[booleanPreferencesKey(BooleanPreferenceKey.IsPlaybackRepeating.name)] ?: false
    }

    val isPlaybackShufflingFlow = context.dataStore.data.map {
        it[booleanPreferencesKey(BooleanPreferenceKey.IsPlaybackShuffling.name)] ?: false
    }

    val userId = context.dataStore.data.map {
        it[stringPreferencesKey(StringPreferenceKey.UserId.name)]
    }
}