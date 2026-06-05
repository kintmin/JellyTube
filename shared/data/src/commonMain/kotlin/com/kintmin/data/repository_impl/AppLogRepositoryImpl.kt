package com.kintmin.data.repository_impl

import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.app_log.repository.AppLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AppLogRepositoryImpl constructor(
    private val fileManager: FileManager,
) : AppLogRepository {

    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logMutex = Mutex()

    override fun appendAppLog(type: String, message: String) {
        logScope.launch {
            logMutex.withLock {
                runCatching {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val fileDate = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
                    val time = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}.${(now.nanosecond / 1_000_000).toString().padStart(3, '0')}"
                    val line = "[$fileDate $time] [$type] $message"
                    fileManager.appendAppLog(fileDate, line).getOrThrow()
                }
            }
        }
    }

    override suspend fun fetchAppLogDateList(): Result<List<String>> = fileManager.fetchAppLogDateList()

    override suspend fun fetchAppLogLineList(date: String): Result<List<String>> = fileManager.fetchAppLogLineList(date)
}
