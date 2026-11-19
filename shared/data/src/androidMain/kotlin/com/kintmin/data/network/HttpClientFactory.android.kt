package com.kintmin.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal actual fun createHttpClient(): HttpClient = HttpClient(Android) { installDefaults() }

/** 같은 네트워크의 기기는 평문 HTTP 다. Android 엔진은 평문을 막는 네트워크 보안 정책을 타지만 CIO 는 소켓을 직접 열어 타지 않는다. */
internal fun createLocalNetworkHttpClient(): HttpClient = HttpClient(CIO) { installDefaults() }

private fun HttpClientConfig<*>.installDefaults() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        connectTimeoutMillis = TIMEOUT_MILLIS
        requestTimeoutMillis = TIMEOUT_MILLIS
        socketTimeoutMillis = TIMEOUT_MILLIS
    }
}

private const val TIMEOUT_MILLIS = 30 * 1000L
