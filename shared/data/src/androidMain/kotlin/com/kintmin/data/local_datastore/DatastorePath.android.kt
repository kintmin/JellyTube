package com.kintmin.data.local_datastore

import android.content.Context

internal fun createAndroidPreferencesDataStore(context: Context) = createPreferencesDataStore {
    context.filesDir
        .resolve("datastore")
        .resolve(DATASTORE_FILE_NAME)
        .absolutePath
}
