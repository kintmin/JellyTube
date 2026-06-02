package com.kintmin.data.local_db.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal fun createIosJellyTubeDatabase(): JellyTubeDatabase {
    return Room.databaseBuilder<JellyTubeDatabase>(
        name = databasePath(),
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}

private fun databasePath(): String {
    val documentDirectory: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    ) ?: error("Document directory is unavailable")

    return documentDirectory.path + "/${JellyTubeDatabase.DB_NAME}"
}
