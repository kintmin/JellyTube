package com.kintmin.data.local_file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/** 플랫폼이 정하는 것은 디렉터리 자리뿐이다. 없으면 [FileManagerCore] 가 만든다. */
internal interface AppDirectories {
    fun audio(): Path
    fun image(): Path
    fun lyric(): Path
    fun log(): Path
    fun migrationStaging(): Path

    companion object {
        const val LYRIC_DIR_NAME = "lyrics"
        const val LOG_DIR_NAME = "app_logs"
        const val MIGRATION_STAGING_DIR_NAME = "migration_staging"
    }
}

/** 두 플랫폼 `FileManagerImpl` 이 같이 쓰는 파일 일. 플랫폼 API 가 필요한 일만 각 구현에 남는다. */
internal class FileManagerCore(
    private val directories: AppDirectories,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    fun audioDir(): Path = directories.audio().ensured()

    fun imageDir(): Path = directories.image().ensured()

    private fun lyricDir(): Path = directories.lyric().ensured()

    private fun logDir(): Path = directories.log().ensured()

    fun getFileNameWithExt(fileFullPath: String): Result<String> = runCatching { fileFullPath.toPath().name }

    fun getAudioDownloadBasePath(fileName: String): Result<String> = runCatching { (audioDir() / fileName).toString() }

    fun getAudioFileFullPath(fileNameWithExt: String): Result<String> = runCatching { (audioDir() / fileNameWithExt).toString() }

    fun getImageFileFullPath(fileNameWithExt: String): Result<String> = runCatching { (imageDir() / fileNameWithExt).toString() }

    fun getLyricFileFullPath(fileNameWithExt: String): Result<String> = runCatching { (lyricDir() / fileNameWithExt).toString() }

    fun getMigrationStagingDirPath(): Result<String> = runCatching { directories.migrationStaging().ensured().toString() }

    suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<String> = io {
        val ext = if (synced) SYNCED_LYRICS_EXT else PLAIN_LYRICS_EXT
        fileSystem.write(lyricDir() / "$fileName.$ext") { writeUtf8(text) }
        ext
    }

    suspend fun fetchLyrics(fileNameWithExt: String): Result<String> = io {
        val file = lyricDir() / fileNameWithExt
        if (!fileSystem.exists(file)) error("가사 파일을 찾을 수 없습니다.")
        fileSystem.read(file) { readUtf8() }
    }

    suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit> = io {
        fileSystem.delete(fileFullPath.toPath(), mustExist = false)
    }

    suspend fun listAudioAndImageFileFullPaths(): Result<List<String>> = io {
        filesIn(audioDir()).map { it.toString() } + filesIn(imageDir()).map { it.toString() }
    }

    suspend fun appendAppLog(date: String, line: String): Result<Unit> = io {
        fileSystem.appendingSink(logDir() / "$date.$LOG_EXT").buffer().use { it.writeUtf8("$line\n") }
        logFilesNewestFirst().drop(MAX_LOG_FILE_COUNT).forEach { fileSystem.delete(it, mustExist = false) }
    }

    suspend fun fetchAppLogDateList(): Result<List<String>> = io {
        logFilesNewestFirst().map { it.name.removeSuffix(".$LOG_EXT") }
    }

    suspend fun fetchAppLogLineList(date: String): Result<List<String>> = io {
        val file = logDir() / "$date.$LOG_EXT"
        if (!fileSystem.exists(file)) return@io emptyList()
        fileSystem.read(file) { readUtf8() }.lines().filter { it.isNotEmpty() }.asReversed()
    }

    suspend fun fetchAppLogFileFullPaths(): Result<List<String>> = io {
        logFilesNewestFirst().map { it.toString() }
    }

    private fun logFilesNewestFirst(): List<Path> =
        filesIn(logDir()).filter { it.name.endsWith(".$LOG_EXT") }.sortedByDescending { it.name }

    private fun filesIn(dir: Path): List<Path> =
        fileSystem.list(dir).filter { fileSystem.metadataOrNull(it)?.isRegularFile == true }

    private fun Path.ensured(): Path = also { fileSystem.createDirectories(it) }

    private suspend fun <T> io(block: () -> T): Result<T> = runCatching { withContext(Dispatchers.IO) { block() } }

    companion object {
        const val SYNCED_LYRICS_EXT = "lrc"
        const val PLAIN_LYRICS_EXT = "txt"
        const val LOG_EXT = "log"
    }
}
