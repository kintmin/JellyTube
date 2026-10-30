package com.kintmin.data.repository_impl

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.local_db.dao.AudioEditProjectDao
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toAudioEditTimelineOrNull
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.mapper.toTimelineJson
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.AudioWaveform
import com.kintmin.domain.audio_edit.model.EditedAudioSource
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_edit.usecase.AudioEditAlreadySaving
import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

internal class AudioEditRepositoryImpl constructor(
    private val audioMediaFacade: AudioMediaFacade,
    private val audioMediaDao: AudioMediaDao,
    private val audioEditProjectDao: AudioEditProjectDao,
    private val fileManager: FileManager,
    private val audioEditor: AudioEditor,
) : AudioEditRepository {

    private companion object {

        /** 편집본 컨테이너는 항상 MP4(m4a)다. 렌더 출력 포맷이 AAC/MP4로 고정되어 있다. */
        const val EDITED_AUDIO_EXT = "m4a"
    }

    private val savingAudioMediaIds = MutableStateFlow<Set<Int>>(emptySet())

    override fun observeSavingAudioMediaIds(): Flow<Set<Int>> = savingAudioMediaIds.asStateFlow()

    override fun reserveSaving(audioMediaId: Int): Boolean {
        val before = savingAudioMediaIds.getAndUpdate { it + audioMediaId }
        return audioMediaId !in before
    }

    override fun releaseSaving(audioMediaId: Int) {
        savingAudioMediaIds.update { it - audioMediaId }
    }

    override suspend fun measureAudioDurationMs(audioFileFullPath: String): Result<Long> =
        audioEditor.measureDurationMs(audioFileFullPath)

    /**
     * [audioMediaId]를 저장 중으로 표시한 채 [block]을 실행하고, 끝나면 표시를 지운다.
     * 중복 저장은 [reserveSaving]에서 막는다. 예약 없이 실행되는 경우(프로세스가 죽은 뒤 작업이 다시 도는 등)도
     * 여기서 표시한다.
     */
    private suspend fun <T> withSavingMark(audioMediaId: Int, block: suspend () -> Result<T>): Result<T> {
        savingAudioMediaIds.update { it + audioMediaId }
        return try {
            block()
        } finally {
            savingAudioMediaIds.update { it - audioMediaId }
        }
    }

    override fun extractWaveform(
        audioFileFullPath: String,
        bucketCount: Int,
    ): Flow<AudioWaveform> = audioEditor.extractWaveform(
        audioFileFullPath = audioFileFullPath,
        bucketCount = bucketCount,
    )

    override suspend fun fetchEditTimeline(audioMediaId: Int): Result<AudioEditTimeline?> =
        withContext(Dispatchers.IO) {
            runCatching {
                audioEditProjectDao.getByAudioMediaId(audioMediaId)?.timelineJson?.toAudioEditTimelineOrNull()
            }
        }

    override suspend fun applyEditedAudio(
        audioMediaId: Int,
        timeline: AudioEditTimeline,
    ): Result<AudioMedia> = withSavingMark(audioMediaId) { applyEditedAudioInternal(audioMediaId, timeline) }

    private suspend fun applyEditedAudioInternal(
        audioMediaId: Int,
        timeline: AudioEditTimeline,
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
                parts = timeline.toParts(),
                outputFileFullPath = editedFileFullPath,
            ).getOrThrow()

            runCatching {
                audioMediaFacade.applyEditedAudio(
                    audioMediaId = audioMediaId,
                    editedAudioFileNameWithExt = editedFileNameWithExt,
                    editedAudioDurationSeconds = editedDurationMs / 1000,
                    timelineJson = timeline.toTimelineJson(),
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

    override suspend fun revertEditedAudio(audioMediaId: Int): Result<AudioMedia> {
        // 저장이 끝나면 편집본이 다시 연결되므로, 저장 중 되돌리기는 결과가 뒤엉킨다
        if (audioMediaId in savingAudioMediaIds.value) return Result.failure(AudioEditAlreadySaving())
        return revertEditedAudioInternal(audioMediaId)
    }

    private suspend fun revertEditedAudioInternal(audioMediaId: Int): Result<AudioMedia> =
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
    ): Result<AudioMedia> = withSavingMark(audioMediaId) {
        saveEditedAudioAsNewMediaInternal(
            audioMediaId = audioMediaId,
            parts = parts,
            name = name,
            playlistIdOnDownload = playlistIdOnDownload,
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
        )
    }

    private suspend fun saveEditedAudioAsNewMediaInternal(
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
