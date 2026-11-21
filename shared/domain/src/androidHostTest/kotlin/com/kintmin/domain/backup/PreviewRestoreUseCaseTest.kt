package com.kintmin.domain.backup

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.RestorePreview
import com.kintmin.domain.backup.usecase.PreviewRestoreUseCase
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewRestoreUseCaseTest {

    private fun audio(id: Int, source: String, durationSeconds: Long?) = MigrationAudioMedia(
        id = id, source = source, name = "곡$id", artist = "", description = "", rawAudioDurationSeconds = durationSeconds,
        audioFileName = "$id.m4a", imageFileName = null, lyricFileName = null, tjKaraokeNumber = null,
        editedAudioFileName = null, editedAudioDurationSeconds = null, createdTimeMillis = id.toLong(),
    )

    private fun snapshot(vararg audios: MigrationAudioMedia) =
        MigrationSnapshot(audios.toList(), emptyList(), emptyList(), emptyList(), emptyList())

    @Test
    fun `같은 곡은 한 번만 세고 이미 있는 곡과 복원할 수 없는 곡을 가른다`() = runTest {
        val repository = mockk<DataMigrationRepository> {
            coEvery { fetchSnapshot() } returns Result.success(snapshot(audio(7, "https://www.youtube.com/watch?v=a", 100)))
        }
        val backup = BackupContent(
            snapshot = snapshot(
                audio(1, "https://youtu.be/a", 100),
                audio(2, "https://youtu.be/b", 100),
                audio(3, "https://youtu.be/b?si=x", 100),
                audio(4, "https://youtu.be/c", null),
                audio(5, "content://media/5", 100),
            ),
            downloadSettings = null,
            lyrics = emptyMap(),
            createdAtMillis = 9,
        )

        val preview = PreviewRestoreUseCase(repository)(backup).getOrThrow()

        assertEquals(
            RestorePreview(
                createdAtMillis = 9,
                totalCount = 4,
                alreadyOnDeviceCount = 1,
                redownloadCount = 2,
                notRestorableCount = 1,
                estimatedDownloadBytes = (100 + 240) * 16_000L,
            ),
            preview,
        )
    }
}
