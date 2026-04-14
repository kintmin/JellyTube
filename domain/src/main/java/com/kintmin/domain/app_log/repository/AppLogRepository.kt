package com.kintmin.domain.app_log.repository

interface AppLogRepository {
    fun appendAppLog(type: String, message: String)
    suspend fun fetchAppLogDateList(): Result<List<String>>
    suspend fun fetchAppLogLineList(date: String): Result<List<String>>
}
