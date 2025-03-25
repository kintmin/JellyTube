package com.kintmin.network.di

internal object RetrofitModule {
    //private const val BASE_URL = "https://dummy.com/"

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