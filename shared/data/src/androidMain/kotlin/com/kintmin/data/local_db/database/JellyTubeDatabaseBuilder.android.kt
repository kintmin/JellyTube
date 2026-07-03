package com.kintmin.data.local_db.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

internal fun createAndroidJellyTubeDatabase(context: Context): JellyTubeDatabase {
    return Room.databaseBuilder<JellyTubeDatabase>(
        context = context,
        name = context.getDatabasePath(JellyTubeDatabase.DB_NAME).absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_3_4)
        .build()
}
