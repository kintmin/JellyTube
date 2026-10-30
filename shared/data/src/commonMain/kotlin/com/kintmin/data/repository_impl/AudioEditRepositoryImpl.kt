package com.kintmin.data.repository_impl

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.audio_edit.model.AudioEditSegment
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class AudioEditRepositoryImpl constructor(
    private val audioMediaFacade: AudioMediaFacade,
    private val audioMediaDao: AudioMediaDao,
    private val fileManager: FileManager,
    private val audioEditor: AudioEditor,
) : AudioEditRepository {

    private companion object {

        /** 편집본 컨테이너는 항상 MP4(m4a)다. 렌더 출력 포맷이 AAC/MP4로 고정되어 있다. */
        const val EDITED_AUDIO_EXT = "m4a"
    }

    override suspend fun extractWaveform(
        audioFileFullPath: String,
        bucketCount: Int,
    ): Result<List<Int>> = audioEditor.extractWaveform(
        audioFileFullPath = audioFileFullPath,
        bucketCount = bucketCount,
    )

    override suspend fun applyEditedAudio(
        audioMediaId: Int,
        segments: List<AudioEditSegment>,
    ): Result<AudioMedia> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = audioMediaDao.getDataById(audioMediaId)
            val sourceFileFullPath = fileManager
                .getAudioFileFullPath(fileNameWithExt = entity.audioFileNameWithExt)
                .getOrThrow()

            val editedFileNameWithExt = "${Uuid.random()}.$EDITED_AUDIO_EXT"
            val editedFileFullPath = fileManager
                .getAudioFileFullPath(fileNameWithExt = editedFileNameWithExt)
                .getOrThrow()

            val editedDurationMs = audioEditor.render(
                sourceFileFullPath = sourceFileFullPath,
                segments = segments,
                outputFileFullPath = editedFileFullPath,
            ).getOrThrow()

            runCatching {
                audioMediaFacade.applyEditedAudio(
                    audioMediaId = audioMediaId,
                    editedAudioFileNameWithExt = editedFileNameWithExt,
                    editedAudioDurationSeconds = editedDurationMs / 1000,
                )
            }.onFailure {
                // DB 연결에 실패한 편집본 파일은 아무도 참조하지 않으므로 바로 지운다
                fileManager.deleteFileAtFullPath(editedFileFullPath)
            }.getOrThrow()

            // DB가 진원지다. 커밋된 뒤에 이전 편집본 파일을 정리한다.
            entity.editedAudioFileNameWithExt?.let { previousFileNameWithExt ->
                deleteAudioFileSafely(previousFileNameWithExt)
            }

            audioMediaDao.getDataById(audioMediaId).toDomain(fileManager).getOrThrow()
        }
    }

    override suspend fun revertEditedAudio(audioMediaId: Int): Result<AudioMedia> =
        withContext(Dispatchers.IO) {
            runCatching {
                val editedFileNameWithExt = audioMediaDao
                    .getDataById(audioMediaId)
                    .editedAudioFileNameWithExt

                audioMediaFacade.clearEditedAudio(audioMediaId = audioMediaId)

                // 커밋 후 파일 정리. 실패해도 고아 파일 정리가 나중에 치운다.
                editedFileNameWithExt?.let { deleteAudioFileSafely(it) }

                audioMediaDao.getDataById(audioMediaId).toDomain(fileManager).getOrThrow()
            }
        }

    private suspend fun deleteAudioFileSafely(fileNameWithExt: String) {
        fileManager.getAudioFileFullPath(fileNameWithExt).onSuccess { fileFullPath ->
            fileManager.deleteFileAtFullPath(fileFullPath)
        }
    }
}
