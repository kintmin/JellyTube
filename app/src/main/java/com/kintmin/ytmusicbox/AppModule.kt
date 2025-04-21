package com.kintmin.ytmusicbox

import android.content.Context
import android.os.Build
import com.kintmin.domain.platform_api.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideLog(): Log {
        return object : Log {
            override fun d(tag: String, message: String) {
                android.util.Log.d(tag, message)
            }
        }
    }
}
