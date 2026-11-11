package com.kintmin.data.repository_impl

import com.kintmin.data.data_migration.toExport
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.network.dataSource.MigrationDataSource
import com.kintmin.domain.data_migration.MigrationFiles
import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPairRejectedException
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationVersionMismatchException
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.fileshare.FileShareConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

class DataMigrationRepositoryImpl constructor(
    private val audioMediaFacade: AudioMediaFacade,
    private val fileManager: FileManager,
    private val migrationDataSource: MigrationDataSource,
) : DataMigrationRepository {

    private val fileSystem = FileSystem.SYSTEM

    override suspend fun exportData(): Result<MigrationExport> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = audioMediaFacade.fetchMigrationSnapshot()
            val files = MigrationFiles.referencedBy(snapshot.audioMedias, snapshot.playlists)
                .mapNotNull { file ->
                    val path = resolveFilePath(file).getOrNull() ?: return@mapNotNull null
                    val size = fileSystem.metadataOrNull(path.toPath())?.size ?: return@mapNotNull null
                    MigrationFileEntry(file, size)
                }
            MigrationExport(snapshot, files, downloadSettings = null)
        }
    }

    override fun resolveFilePath(file: MigrationFile): Result<String> = when (file.kind) {
        MigrationFileKind.AUDIO -> fileManager.getAudioFileFullPath(file.name)
        MigrationFileKind.IMAGE -> fileManager.getImageFileFullPath(file.name)
        MigrationFileKind.LYRIC -> fileManager.getLyricFileFullPath(file.name)
    }

    override suspend fun fetchSnapshot(): Result<MigrationSnapshot> = withContext(Dispatchers.IO) {
        runCatching { audioMediaFacade.fetchMigrationSnapshot() }
    }

    override suspend fun pair(endpoint: MigrationEndpoint, code: String): Result<String> = runCatching {
        migrationDataSource.pair(endpoint.host, endpoint.port, code).getOrThrow()
            ?: throw MigrationPairRejectedException()
    }

    override suspend fun fetchRemoteExport(endpoint: MigrationEndpoint, token: String): Result<MigrationExport> =
        runCatching {
            val manifest = migrationDataSource.fetchManifest(endpoint.host, endpoint.port, token).getOrThrow()
            if (manifest.formatVersion != FileShareConstants.MIGRATION_FORMAT_VERSION) {
                throw MigrationVersionMismatchException()
            }
            manifest.toExport()
        }

    override suspend fun stageFile(
        endpoint: MigrationEndpoint,
        token: String,
        entry: MigrationFileEntry,
        onReceived: (Long) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val staged = stagedPath(entry.file)
            if (fileSystem.metadataOrNull(staged)?.size == entry.size) return@runCatching

            // 받다 끊긴 파일이 받아 둔 파일로 보이지 않도록 다 받은 뒤에 이름을 바꾼다.
            val part = "$staged.part".toPath()
            fileSystem.createDirectories(staged.parent!!)
            var received = 0L
            val sink = fileSystem.sink(part).buffer()
            try {
                migrationDataSource.downloadFile(
                    host = endpoint.host,
                    port = endpoint.port,
                    token = token,
                    kind = entry.file.kind.name,
                    name = entry.file.name,
                ) { bytes, count ->
                    sink.write(bytes, 0, count)
                    received += count
                    onReceived(received)
                }.getOrThrow()
            } finally {
                sink.close()
            }
            check(received == entry.size) { "${entry.file.name} 크기가 다릅니다: $received / ${entry.size}" }
            fileSystem.atomicMove(part, staged)
        }
    }

    override suspend fun applyPlan(plan: MigrationPlan): Result<MigrationApplyResult> = withContext(Dispatchers.IO) {
        runCatching {
            val moved = mutableListOf<Path>()
            try {
                plan.requiredFiles.forEach { file ->
                    val destination = resolveFilePath(file).getOrThrow().toPath()
                    // 이름이 UUID 라 이미 있으면 같은 파일이다. 남의 파일이므로 되돌릴 때 지우지 않는다.
                    if (fileSystem.exists(destination)) return@forEach
                    move(stagedPath(file), destination)
                    moved += destination
                }
                MigrationApplyResult(audioMediaFacade.applyMigrationPlan(plan))
            } catch (e: Throwable) {
                moved.forEach { fileSystem.delete(it, mustExist = false) }
                throw e
            }
        }
    }

    override suspend fun clearStaging(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            fileSystem.deleteRecursively(fileManager.getMigrationStagingDirPath().getOrThrow().toPath(), mustExist = false)
        }
    }

    private fun stagedPath(file: MigrationFile): Path =
        fileManager.getMigrationStagingDirPath().getOrThrow().toPath() / file.kind.name / file.name

    private fun move(source: Path, destination: Path) {
        try {
            fileSystem.atomicMove(source, destination)
        } catch (e: IOException) {
            fileSystem.copy(source, destination)
            fileSystem.delete(source)
        }
    }
}
