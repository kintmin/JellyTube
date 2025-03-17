package com.kintmin.ytmusicbox.data.local

import android.content.Context
import androidx.room.Room
import com.kintmin.ytmusicbox.data.local.dao.YoutubeMediaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DB_NAME
    ).build()

    @Provides
    fun provideVideoDao(database: AppDatabase): YoutubeMediaDao {
        return database.videoDao()
    }
}