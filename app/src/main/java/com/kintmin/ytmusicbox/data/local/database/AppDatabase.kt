package com.kintmin.ytmusicbox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kintmin.ytmusicbox.data.local.dao.YoutubeMediaDao
import com.kintmin.ytmusicbox.data.local.entity.YoutubeMediaEntity

@Database(entities = [YoutubeMediaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): YoutubeMediaDao

    companion object {
        const val DB_NAME = "app_database"
    }
}
