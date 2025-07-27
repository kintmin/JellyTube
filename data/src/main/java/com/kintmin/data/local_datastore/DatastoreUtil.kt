package com.kintmin.data.local_datastore

import kotlinx.coroutines.flow.Flow

interface DatastoreUtil {

    suspend fun <T> updateData(preferencesKey: PreferencesKey<T>, newData: T): Result<Unit>
    fun <T> getData(preferencesKey: PreferencesKey<T>): Flow<T?>
}