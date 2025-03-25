package com.kintmin.localdatabase.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kintmin.localdatabase.dao.AudioMediaDao
import com.kintmin.localdatabase.dao.PlaylistDao
import com.kintmin.localdatabase.entity.AudioMediaEntity
import com.kintmin.localdatabase.entity.PlaylistEntity

@Database(
    entities = [AudioMediaEntity::class, PlaylistEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioMediaDao(): AudioMediaDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DB_NAME = "app_database"
    }
}
