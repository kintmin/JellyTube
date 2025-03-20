package com.kintmin.ytmusicbox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kintmin.data.local.dao.AudioMediaDao
import com.kintmin.data.local.entity.AudioMediaEntity

@Database(entities = [AudioMediaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioMediaDao(): AudioMediaDao

    companion object {
        const val DB_NAME = "app_database"
    }
}
