package com.kintmin.domain.app_log.repository

interface AppLogRepository {
    fun appendAppLog(type: String, message: String)
    suspend fun fetchAppLogDateList(): Result<List<String>>
    suspend fun fetchAppLogLineList(date: String): Result<List<String>>

    /** 최신 날짜부터. 공유처럼 파일 자체가 필요한 곳이 쓴다. */
    suspend fun fetchAppLogFileFullPaths(): Result<List<String>>
}
