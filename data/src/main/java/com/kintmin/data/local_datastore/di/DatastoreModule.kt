package com.kintmin.data.local_datastore.di

import android.content.Context
import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.DatastoreUtilImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    @Provides
    @Singleton
    fun provideDatastore(
        @ApplicationContext context: Context,
    ): DatastoreUtil = DatastoreUtilImpl(context)
}