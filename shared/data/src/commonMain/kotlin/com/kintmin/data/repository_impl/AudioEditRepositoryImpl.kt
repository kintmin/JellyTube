package com.kintmin.data.repository_impl

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioWaveform
import com.kintmin.domain.audio_edit.model.EditedAudioSource
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
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

    override fun extractWaveform(
        audioFileFullPath: String,
        bucketCount: Int,
    ): Flow<AudioWaveform> = audioEditor.extractWaveform(
        audioFileFullPath = audioFileFullPath,
        bucketCount = bucketCount,
    )

    override suspend fun applyEditedAudio(
        audioMediaId: Int,
        parts: List<AudioEditPart>,
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
                parts = parts,
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

    override suspend fun saveEditedAudioAsNewMedia(
        audioMediaId: Int,
        parts: List<AudioEditPart>,
        name: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AudioMedia> = withContext(Dispatchers.IO) {
        runCatching {
            val originEntity = audioMediaDao.getDataById(audioMediaId)
            val sourceFileFullPath = fileManager
                .getAudioFileFullPath(fileNameWithExt = originEntity.audioFileNameWithExt)
                .getOrThrow()

            val newFileName = Uuid.random().toString()
            val newFileNameWithExt = "$newFileName.$EDITED_AUDIO_EXT"
            val newFileFullPath = fileManager
                .getAudioFileFullPath(fileNameWithExt = newFileNameWithExt)
                .getOrThrow()

            val durationMs = audioEditor.render(
                sourceFileFullPath = sourceFileFullPath,
                parts = parts,
                outputFileFullPath = newFileFullPath,
            ).getOrThrow()

            // 썸네일은 파일을 복사해서 물려준다. 파일명을 공유하면 한쪽을 지울 때 다른 쪽 썸네일이 사라진다.
            val newImageFileNameWithExt = originEntity.imageFileNameWithExt?.let { originImage ->
                fileManager.copyImageFile(
                    sourceFileNameWithExt = originImage,
                    targetFileName = newFileName,
                ).getOrNull()?.let { ext -> "$newFileName.$ext" }
            }

            val entity = AudioMediaEntity(
                source = EditedAudioSource.build(
                    uniqueId = newFileName,
                    originSource = originEntity.source,
                ),
                name = name,
                artist = originEntity.artist,
                description = originEntity.description,
                rawAudioDurationSeconds = durationMs / 1000,
                audioFileNameWithExt = newFileNameWithExt,
                imageFileNameWithExt = newImageFileNameWithExt,
                // 가사는 길이가 달라져 맞지 않으므로 가져오지 않는다
                lyricFileNameWithExt = null,
                tjKaraokeNumber = originEntity.tjKaraokeNumber,
            )

            val added = runCatching {
                audioMediaFacade.addNewAudioMedia(
                    newAudioMedia = entity,
                    requestedPlaylistId = playlistIdOnDownload,
                    shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
                )
            }.onFailure {
                // 등록되지 않은 파일은 아무도 참조하지 않으므로 바로 지운다
                fileManager.deleteFileAtFullPath(newFileFullPath)
                newImageFileNameWithExt?.let { deleteImageFileSafely(it) }
            }.getOrThrow()

            entity.copy(id = added.audioMediaId).toDomain(fileManager).getOrThrow()
        }
    }

    private suspend fun deleteImageFileSafely(fileNameWithExt: String) {
        fileManager.getImageFileFullPath(fileNameWithExt).onSuccess { fileFullPath ->
            fileManager.deleteFileAtFullPath(fileFullPath)
        }
    }

    private suspend fun deleteAudioFileSafely(fileNameWithExt: String) {
        fileManager.getAudioFileFullPath(fileNameWithExt).onSuccess { fileFullPath ->
            fileManager.deleteFileAtFullPath(fileFullPath)
        }
    }
}
