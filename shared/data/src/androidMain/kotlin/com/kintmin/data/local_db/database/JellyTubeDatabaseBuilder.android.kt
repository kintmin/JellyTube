package com.kintmin.data.local_db.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kintmin.domain.playlist.model.Playlist
import kotlinx.coroutines.Dispatchers

internal fun createAndroidJellyTubeDatabase(context: Context): JellyTubeDatabase {
    return Room.databaseBuilder<JellyTubeDatabase>(
        context = context,
        name = context.getDatabasePath(JellyTubeDatabase.DB_NAME).absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addCallback(initialPlaylistCallback())
        .build()
}

private fun initialPlaylistCallback(): RoomDatabase.Callback {
    return object : RoomDatabase.Callback() {
        override fun onCreate(connection: SQLiteConnection) {
            connection.execSQL(
                """
INSERT INTO PLAYLIST (id, name, description, audioMediaCount, rawPlayTimeDuration, rawCreatedTime, imageFileNameWithExt, isCustomImage)
VALUES (${Playlist.TOTAL}, '전체', '', 0, 0, strftime('%s','now') * 1000, null, 0)
                """.trimIndent()
            )
            connection.execSQL(
                """
INSERT INTO PLAYLIST (id, name, description, audioMediaCount, rawPlayTimeDuration, rawCreatedTime, imageFileNameWithExt, isCustomImage)
VALUES (${Playlist.UNCATEGORIZED}, '미분류', '', 0, 0, strftime('%s','now') * 1000, null, 0)
                """.trimIndent()
            )
        }
    }
}
