package com.kintmin.data.local_datastore

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal fun createIosPreferencesDataStore() = createPreferencesDataStore {
    val documentDirectory: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    ) ?: error("Document directory is unavailable")

    documentDirectory.path + "/$DATASTORE_FILE_NAME"
}
