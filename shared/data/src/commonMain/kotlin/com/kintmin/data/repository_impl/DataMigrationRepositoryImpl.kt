package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.data_migration.MigrationFiles
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath

class DataMigrationRepositoryImpl constructor(
    private val audioMediaFacade: AudioMediaFacade,
    private val fileManager: FileManager,
) : DataMigrationRepository {

    override suspend fun exportData(): Result<MigrationExport> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = audioMediaFacade.fetchMigrationSnapshot()
            val files = MigrationFiles.referencedBy(snapshot.audioMedias, snapshot.playlists)
                .mapNotNull { file ->
                    val path = resolveFilePath(file).getOrNull() ?: return@mapNotNull null
                    val size = FileSystem.SYSTEM.metadataOrNull(path.toPath())?.size ?: return@mapNotNull null
                    MigrationFileEntry(file, size)
                }
            MigrationExport(snapshot, files)
        }
    }

    override fun resolveFilePath(file: MigrationFile): Result<String> = when (file.kind) {
        MigrationFileKind.AUDIO -> fileManager.getAudioFileFullPath(file.name)
        MigrationFileKind.IMAGE -> fileManager.getImageFileFullPath(file.name)
        MigrationFileKind.LYRIC -> fileManager.getLyricFileFullPath(file.name)
    }
}
