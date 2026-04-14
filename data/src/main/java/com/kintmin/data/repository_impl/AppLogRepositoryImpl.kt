package com.kintmin.data.repository_impl

import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.app_log.repository.AppLogRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppLogRepositoryImpl @Inject constructor(
    private val fileManager: FileManager,
) : AppLogRepository {

    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logMutex = Mutex()
    private val logDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val logLineDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    override fun appendAppLog(type: String, message: String) {
        logScope.launch {
            logMutex.withLock {
                runCatching {
                    val now = LocalDateTime.now()
                    val fileDate = LocalDate.now().format(logDateFormatter)
                    val line = "[${now.format(logLineDateTimeFormatter)}] [$type] $message"
                    fileManager.appendAppLog(fileDate, line).getOrThrow()
                }
            }
        }
    }

    override suspend fun fetchAppLogDateList(): Result<List<String>> = fileManager.fetchAppLogDateList()

    override suspend fun fetchAppLogLineList(date: String): Result<List<String>> = fileManager.fetchAppLogLineList(date)
}
