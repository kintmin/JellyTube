package com.kintmin.data.local_datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DatastoreUtilImpl(
    val context: Context,
): DatastoreUtil {

    private companion object {
        const val DATASTORE_NAME = "settings"
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

    override suspend fun <T> updateData(
        preferencesKey: PreferencesKey<T>,
        newData: T
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching<Unit> {
                context.dataStore.edit { settings ->
                    settings[preferencesKey.key] = newData
                }
            }
        }
    }

    override fun <T> getData(preferencesKey: PreferencesKey<T>): Flow<T?> {
        return context.dataStore.data.map {
            it[preferencesKey.key]
        }
    }
}