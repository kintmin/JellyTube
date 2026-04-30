package com.kintmin.data.local_db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.dao.StepDao
import com.kintmin.data.local_db.model.*

@Database(
    entities = [
        AudioMediaEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        StepEntity::class,
    ],
    exportSchema = true,
    version = 1,
    autoMigrations = [],
)
abstract class JellyTubeDatabase : RoomDatabase() {

    abstract fun audioMediaDao(): AudioMediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun stepDao(): StepDao

    companion object {

        const val DB_NAME = "jelly_tube_database"
    }
}
