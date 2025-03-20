package com.kintmin.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kintmin.data.remote.datasource.HttpDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RetrofitModule {

    private const val TIMEOUT = 30000L
    //private const val BASE_URL = "https://dummy.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideHttpDataSource(
        defaultClient: OkHttpClient,
    ): HttpDataSource {
        return HttpDataSource(defaultClient)
    }

//    @Provides
//    @Singleton
//    fun provideRetrofit(
//        defaultClient: OkHttpClient,
//    ): Retrofit {
//        return Retrofit.Builder()
//            .client(
//                defaultClient.newBuilder().addInterceptor(Interceptor { chain ->
//                    return@Interceptor chain.proceed(
//                        chain.request()
//                            .newBuilder()
//                            //.addHeader("Authorization", "Bearer ${token}")
//                            .build()
//                    )
//                }).build()
//            )
//            .baseUrl(BASE_URL)
//            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
//            .build()
//    }
}