package com.kintmin.data.local_db.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal fun createIosJellyTubeDatabase(): JellyTubeDatabase {
    return Room.databaseBuilder<JellyTubeDatabase>(
        name = databasePath(),
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_3_4)
        .build()
}

@OptIn(ExperimentalForeignApi::class)
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
