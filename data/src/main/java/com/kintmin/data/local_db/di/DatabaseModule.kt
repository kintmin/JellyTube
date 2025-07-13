package com.kintmin.data.local_db.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kintmin.data.local_db.database.JellyTubeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context,
        JellyTubeDatabase::class.java,
        JellyTubeDatabase.DB_NAME,
    )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("""
INSERT INTO PLAYLIST (id, name, description, audioMediaCount, rawPlayTimeDuration, rawCreatedTime, imageFileNameWithExt, isCustomImage) 
VALUES (1, '전체', '', 0, 0, strftime('%s','now') * 1000, null, 0)
                """.trimIndent())
                db.execSQL("""
INSERT INTO PLAYLIST (id, name, description, audioMediaCount, rawPlayTimeDuration, rawCreatedTime, imageFileNameWithExt, isCustomImage)
VALUES (2, '미분류', '', 0, 0, strftime('%s','now') * 1000, null, 0)
        """.trimIndent())
            }
        })
        .build()
}