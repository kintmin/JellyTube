package com.kintmin.data.local_db.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kintmin.domain.playlist.model.Playlist
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
        .addCallback(initialPlaylistCallback())
        .build()
}

private fun initialPlaylistCallback(): RoomDatabase.Callback {
    return object : RoomDatabase.Callback() {
        override fun onCreate(connection: SQLiteConnection) {
            insertBasePlaylists(connection)
        }

        override fun onOpen(connection: SQLiteConnection) {
            insertBasePlaylists(connection)
        }
    }
}

private fun insertBasePlaylists(connection: SQLiteConnection) {
    connection.execSQL(
        """
INSERT OR IGNORE INTO PLAYLIST (id, name, description, audioMediaCount, rawPlayTimeDuration, rawCreatedTime, imageFileNameWithExt, isCustomImage)
VALUES (${Playlist.TOTAL}, '전체', '', 0, 0, strftime('%s','now') * 1000, null, 0)
        """.trimIndent()
    )
    connection.execSQL(
        """
INSERT OR IGNORE INTO PLAYLIST (id, name, description, audioMediaCount, rawPlayTimeDuration, rawCreatedTime, imageFileNameWithExt, isCustomImage)
VALUES (${Playlist.UNCATEGORIZED}, '미분류', '', 0, 0, strftime('%s','now') * 1000, null, 0)
        """.trimIndent()
    )
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
