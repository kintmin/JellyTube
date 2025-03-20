package com.kintmin.data

import com.kintmin.data.repositoryImpl.YoutubeMediaRepositoryImpl
import com.kintmin.domain.repository.YoutubeMediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindYoutubeMediaRepository(
        repository: YoutubeMediaRepositoryImpl,
    ): YoutubeMediaRepository
}