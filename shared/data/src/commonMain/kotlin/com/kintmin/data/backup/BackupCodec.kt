package com.kintmin.data.backup

import com.kintmin.data.data_migration.toDomain
import com.kintmin.data.data_migration.toDto
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFileInvalidException
import com.kintmin.domain.backup.model.BackupVersionUnsupportedException
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.fileshare.MigrationDownloadSettingsDto
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.Sink
import okio.Source
import okio.buffer
import okio.gzip
import okio.use

internal object BackupCodec {

    const val FORMAT_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(content: BackupContent): ByteArray {
        val dto = BackupDto(
            backupVersion = FORMAT_VERSION,
            createdAtMillis = content.createdAtMillis,
            snapshot = content.snapshot.toDto(),
            downloadSettings = content.downloadSettings?.let { MigrationDownloadSettingsDto(it.shouldInsertAtTop, it.playlistId) },
            lyrics = content.lyrics,
        )
        val compressed = Buffer()
        (compressed as Sink).gzip().buffer().use { it.writeUtf8(json.encodeToString(BackupDto.serializer(), dto)) }
        return compressed.readByteArray()
    }

    /** 옛 버전은 읽는다. 더 새 버전은 모르는 뜻이 담겼을 수 있어 읽지 않는다. */
    fun decode(bytes: ByteArray): BackupContent {
        val dto = try {
            val text = (Buffer().write(bytes) as Source).gzip().buffer().use { it.readUtf8() }
            json.decodeFromString(BackupDto.serializer(), text)
        } catch (e: Exception) {
            throw BackupFileInvalidException(e)
        }
        if (dto.backupVersion > FORMAT_VERSION) throw BackupVersionUnsupportedException()
        return BackupContent(
            snapshot = dto.snapshot.toDomain(),
            downloadSettings = dto.downloadSettings?.let { MigrationDownloadSettings(it.shouldInsertAtTop, it.playlistId) },
            lyrics = dto.lyrics,
            createdAtMillis = dto.createdAtMillis,
        )
    }
}
