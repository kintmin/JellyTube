package com.kintmin.data.local_db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.entity.AudioMediaEntity
import com.kintmin.data.local_db.entity.PlaylistEntity

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
