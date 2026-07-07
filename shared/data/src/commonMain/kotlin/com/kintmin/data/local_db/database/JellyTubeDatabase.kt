package com.kintmin.data.local_db.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
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
    version = 6,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        // 3 -> 4 는 MIGRATION_3_4 참고
        AutoMigration(from = 4, to = 5),
        // 5 -> 6: AUDIO_MEDIA에 tjKaraokeNumber 컬럼(nullable) 추가
        AutoMigration(from = 5, to = 6),
    ],
)
@ConstructedBy(JellyTubeDatabaseConstructor::class)
abstract class JellyTubeDatabase : RoomDatabase() {

    abstract fun audioMediaDao(): AudioMediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun stepDao(): StepDao

    companion object {

        const val DB_NAME = "jelly_tube_database"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object JellyTubeDatabaseConstructor : RoomDatabaseConstructor<JellyTubeDatabase> {
    override fun initialize(): JellyTubeDatabase
}
