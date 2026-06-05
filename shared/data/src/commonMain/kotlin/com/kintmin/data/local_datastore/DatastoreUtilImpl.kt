package com.kintmin.data.local_datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath

class DatastoreUtilImpl(
    private val dataStore: DataStore<Preferences>,
): DatastoreUtil {

    override suspend fun <T> updateData(
        preferencesKey: PreferencesKey<T>,
        newData: T
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                dataStore.edit { settings ->
                    settings[preferencesKey.key] = newData
                }
            }
        }
    }

    override fun <T> getData(preferencesKey: PreferencesKey<T>): Flow<T?> {
        return dataStore.data.map {
            it[preferencesKey.key]
        }
    }
}

internal const val DATASTORE_FILE_NAME = "settings.preferences_pb"

internal fun createPreferencesDataStore(
    producePath: () -> String,
): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() },
    )
}
