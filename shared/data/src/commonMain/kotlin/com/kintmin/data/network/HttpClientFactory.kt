package com.kintmin.data.network

import io.ktor.client.HttpClient

internal expect fun createHttpClient(): HttpClient
