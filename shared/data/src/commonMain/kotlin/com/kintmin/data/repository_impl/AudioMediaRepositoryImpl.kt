package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao_facade.AddNewAudioMediaResult
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.clampText
import com.kintmin.data.local_file.AudioFileExtensionCorrector
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.network.data_source.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.python_bridge.YtDlpError
import com.kintmin.data.python_bridge.model.parseYoutubePlaylist
import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.AudioMediaExportStatus
import com.kintmin.domain.audio_media.model.DownloadedAudioFile
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.model.DownloadedYoutubeAudio
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.lyrics.LyricsFile
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.youtube.model.YoutubePlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class AudioMediaRepositoryImpl constructor(
    private val audioMediaFacade: AudioMediaFacade,
    private val audioMediaDao: AudioMediaDao,
    private val httpDataSource: HttpDataSource,
    private val fileManager: FileManager,
    private val pythonExecutor: PythonExecutor,
) : AudioMediaRepository {

    override suspend fun downloadYoutubeAudio(downloadUrl: String, isSecret: Boolean): Result<DownloadedYoutubeAudio> {
        val fileName = Uuid.random().toString()
        return try {
            downloadYoutubeAudio(downloadUrl, isSecret, fileName)
        } catch (e: CancellationException) {
            // 받기 호출은 도중에 끊기지 않아 파일은 생기고 결과만 버려진다. 이 이름으로 생긴 파일을 지운다.
            withContext(NonCancellable + Dispatchers.IO) { deleteFilesNamed(fileName) }
            throw e
        }
    }

    private suspend fun deleteFilesNamed(fileName: String) {
        fileManager.listAudioAndImageFileFullPaths().getOrNull().orEmpty()
            .filter { fileManager.getFileNameWithExt(it).getOrNull()?.startsWith(fileName) == true }
            .forEach { fileManager.deleteFileAtFullPath(it) }
    }

    private suspend fun downloadYoutubeAudio(downloadUrl: String, isSecret: Boolean, fileName: String): Result<DownloadedYoutubeAudio> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val downloadDto = pythonExecutor.downloadYoutubeMedia(
                    youtubeUrl = downloadUrl,
                    audioDownloadPath = fileManager.getAudioDownloadBasePath(fileName = fileName).getOrThrow(),
                    isSecret = isSecret,
                ).getOrElse { throw YtDlpError.classify(it) }
                DownloadedYoutubeAudio(
                    fileName = fileName,
                    audioFileNameWithExt = downloadDto.audioFileNameWithExt.ifBlank {
                        throw Exception("다운로드한 오디오 파일명을 확인할 수 없습니다.")
                    },
                    title = downloadDto.title,
                    thumbnailUrl = downloadDto.thumbnailDownloadUrl,
                    duration = downloadDto.duration,
                    uploader = downloadDto.uploader,
                    description = downloadDto.description,
                )
            }
        }

    override suspend fun fetchYoutubePlaylist(playlistUrl: String): Result<YoutubePlaylist> =
        withContext(Dispatchers.IO) {
            suspendRunCatching { parseYoutubePlaylist(pythonExecutor.extractYoutubePlaylist(playlistUrl).getOrThrow()) }
        }

    override suspend fun saveThumbnail(thumbnailUrl: String, fileName: String): Result<String> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val image = httpDataSource.downloadImage(imageUrl = thumbnailUrl).getOrThrow()
                val ext = fileManager.saveImageWithCompression(imageData = image, fileName = fileName).getOrThrow()
                "$fileName.$ext"
            }
        }

    override suspend fun downloadAudioFile(downloadUrl: String): Result<DownloadedAudioFile> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val audioDownloadBasePath = fileManager.getAudioDownloadBasePath(
                    fileName = Uuid.random().toString(),
                ).getOrThrow()
                val downloadDto = pythonExecutor.downloadPlayableAudio(
                    youtubeUrl = downloadUrl,
                    audioDownloadPath = audioDownloadBasePath,
                ).getOrThrow()
                val downloadedName = downloadDto.audioFileNameWithExt.ifBlank {
                    throw Exception("다운로드한 오디오 파일명을 확인할 수 없습니다.")
                }
                // HLS 로 받으면 ADTS 가 `.mp4` 로 저장된다. AVAudioFile 은 확장자를 힌트로 쓴다.
                val audioFileNameWithExt = AudioFileExtensionCorrector(audioMediaDao, fileManager)
                    .renameToMatchContent(downloadedName) ?: downloadedName
                DownloadedAudioFile(
                    fileNameWithExt = audioFileNameWithExt,
                    fileFullPath = fileManager.getAudioFileFullPath(audioFileNameWithExt).getOrThrow(),
                    durationSeconds = downloadDto.duration.toLongOrNull(),
                )
            }
        }

    override suspend fun addAudioMedia(
        downloadedAudioMedia: DownloadedMedia,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val audioMediaEntityToSave = AudioMediaEntity(
                    source = downloadedAudioMedia.downloadUrl,
                    name = downloadedAudioMedia.title,
                    artist = downloadedAudioMedia.uploader,
                    description = downloadedAudioMedia.description,
                    rawAudioDurationSeconds = downloadedAudioMedia.duration.toLongOrNull(),
                    audioFileNameWithExt = downloadedAudioMedia.audioFileNameWithExt,
                    imageFileNameWithExt = downloadedAudioMedia.imageFileNameWithExt,
                ).clampText()
                val added = audioMediaFacade.addNewAudioMedia(
                    newAudioMedia = audioMediaEntityToSave,
                    requestedPlaylistId = playlistIdOnDownload,
                    shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
                )
                added.toAddedAudioMedia(audioMediaEntityToSave)
            }
        }
    }

    private val downloadingSources = MutableStateFlow<Set<String>>(emptySet())

    override fun reserveDownload(source: String): Boolean = source !in downloadingSources.getAndUpdate { it + source }

    override fun releaseDownload(source: String) {
        downloadingSources.update { it - source }
    }

    override suspend fun getAudioMediaBySource(source: String): Result<AudioMedia> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                audioMediaDao.getDataBySource(source).toDomain(fileManager).getOrThrow()
            }
        }
    }

    override suspend fun getAudioMedia(id: Int): Result<AudioMedia> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                audioMediaDao.getDataById(id).toDomain(fileManager).getOrThrow()
            }
        }
    }

    override fun getAudioMediaListFlow(): Flow<List<AudioMedia>> {
        return audioMediaDao.getAudioMediaListFlow().map { audioMediaList ->
            audioMediaList.map {
                it.toDomain(fileManager).getOrThrow()
            }
        }
    }

    override suspend fun setCustomImage(id: Int, imageData: ByteArray): Result<Unit> =
        saveImageAndSwap(imageData) { fileNameWithExt -> audioMediaFacade.applyCustomImage(id, fileNameWithExt) }

    override suspend fun replaceImage(id: Int, imageData: ByteArray): Result<Unit> =
        saveImageAndSwap(imageData) { fileNameWithExt -> audioMediaFacade.replaceImage(id, fileNameWithExt) }

    override suspend fun clearCustomImage(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                audioMediaFacade.clearCustomImage(id).forEach { deleteImageFile(it) }
            }
        }
    }

    /**
     * 파일을 먼저 저장하고 DB 를 바꾼다. DB 가 실패하면 방금 저장한 파일은 아무도 참조하지 않으므로
     * 그 자리에서 지운다. [swap] 이 돌려주는 옛 파일명들은 바뀐 뒤 참조가 끊긴 것이다.
     */
    private suspend fun saveImageAndSwap(
        imageData: ByteArray,
        swap: suspend (fileNameWithExt: String) -> List<String>,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val fileName = Uuid.random().toString()
                val ext = fileManager.saveImageWithCompression(imageData, fileName).getOrThrow()
                val fileNameWithExt = "$fileName.$ext"
                suspendRunCatching { swap(fileNameWithExt) }
                    .onFailure { deleteImageFile(fileNameWithExt) }
                    .getOrThrow()
                    .forEach { deleteImageFile(it) }
            }
        }
    }

    override suspend fun updateKaraokeNumber(id: Int, tjKaraokeNumber: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                audioMediaDao.updateKaraokeNumber(id, tjKaraokeNumber)
            }
        }
    }

    override suspend fun clearKaraokeNumber(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                audioMediaDao.clearKaraokeNumber(id)
            }
        }
    }

    override suspend fun saveLyrics(text: String, synced: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val fileName = Uuid.random().toString()
                val ext = fileManager.saveLyrics(text, fileName, synced).getOrThrow()
                fileManager.getLyricFileFullPath(fileNameWithExt = "$fileName.$ext").getOrThrow()
            }
        }
    }

    override suspend fun getLyrics(lyricFileFullPath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val fileNameWithExt = fileManager.getFileNameWithExt(lyricFileFullPath).getOrThrow()
                fileManager.fetchLyrics(fileNameWithExt).getOrThrow()
            }
        }
    }

    override suspend fun saveVariantLyrics(
        baseLyricFileFullPath: String,
        variant: LyricsVariant,
        text: String,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val (baseName, extName) = splitLyricFileName(baseLyricFileFullPath)
                val synced = extName.equals(LyricsFile.SYNCED_EXT, ignoreCase = true)
                fileManager.saveLyrics(text, "$baseName.${variant.infix()}", synced).getOrThrow()
                Unit
            }
        }
    }

    override suspend fun getVariantLyrics(
        baseLyricFileFullPath: String,
        variant: LyricsVariant,
    ): Result<String?> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val (baseName, extName) = splitLyricFileName(baseLyricFileFullPath)
                val variantFileNameWithExt = "$baseName.${variant.infix()}.$extName"
                fileManager.fetchLyrics(variantFileNameWithExt).getOrNull()
            }
        }
    }

    override suspend fun deleteVariantLyrics(
        baseLyricFileFullPath: String,
        variant: LyricsVariant,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val (baseName, extName) = splitLyricFileName(baseLyricFileFullPath)
                val variantFileNameWithExt = "$baseName.${variant.infix()}.$extName"
                deleteLyricFile(variantFileNameWithExt).getOrThrow()
                Unit
            }
        }
    }

    private fun splitLyricFileName(baseLyricFileFullPath: String): Pair<String, String> =
        splitLyricFileNameWithExt(fileManager.getFileNameWithExt(baseLyricFileFullPath).getOrThrow())

    private fun splitLyricFileNameWithExt(nameWithExt: String): Pair<String, String> {
        val dotIndex = nameWithExt.lastIndexOf('.')
        if (dotIndex == -1) throw Exception("가사 파일명에서 확장자를 찾을 수 없습니다.")
        return nameWithExt.substring(0, dotIndex) to nameWithExt.substring(dotIndex + 1)
    }

    private fun LyricsVariant.infix(): String = when (this) {
        LyricsVariant.TRANSLATION -> "translate"
        LyricsVariant.TRANSLITERATION -> "transliterate"
    }

    override suspend fun deleteLyrics(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val oldLyricFileNameWithExt = audioMediaDao.getDataById(id).lyricFileNameWithExt
                audioMediaDao.clearLyricFile(id)
                oldLyricFileNameWithExt?.let { deleteLyricFile(it) }
                Unit
            }
        }
    }

    override suspend fun applyEditedLyrics(id: Int, lyricFileFullPath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val newFileNameWithExt = fileManager.getFileNameWithExt(lyricFileFullPath).getOrThrow()
                val oldFileNameWithExt = audioMediaDao.getDataById(id).editedLyricFileNameWithExt
                audioMediaDao.applyEditedLyricFile(id, newFileNameWithExt)
                // DB가 진원지다. 커밋된 뒤에 아무도 가리키지 않게 된 옛 파일을 치운다.
                if (oldFileNameWithExt != null && oldFileNameWithExt != newFileNameWithExt) {
                    deleteLyricWithVariants(oldFileNameWithExt)
                }
            }
        }
    }

    override suspend fun clearEditedLyrics(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val oldFileNameWithExt = audioMediaDao.getDataById(id).editedLyricFileNameWithExt
                    ?: return@suspendRunCatching
                audioMediaDao.clearEditedLyricFile(id)
                deleteLyricWithVariants(oldFileNameWithExt)
            }
        }
    }

    /** 가사 파일과 그 이름에서 파생된 변형 파일을 함께 지운다. 삭제 실패는 무시한다. */
    private suspend fun deleteLyricWithVariants(fileNameWithExt: String) {
        val (baseName, extName) = splitLyricFileNameWithExt(fileNameWithExt)
        deleteLyricFile(fileNameWithExt)
        LyricsVariant.entries.forEach { variant ->
            deleteLyricFile("$baseName.${variant.infix()}.$extName")
        }
    }

    override suspend fun updateAudioMedia(
        id: Int,
        name: String?,
        artist: String?,
        description: String?,
        lyricFileFullPath: String?,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val newLyricFileNameWithExt = lyricFileFullPath?.let {
                    fileManager.getFileNameWithExt(it).getOrThrow()
                }
                // 가사를 교체하는 경우, 교체 후 orphan이 될 옛 파일명을 미리 확보한다.
                val oldLyricFileNameWithExt = newLyricFileNameWithExt?.let {
                    audioMediaDao.getDataById(id).lyricFileNameWithExt
                }

                audioMediaFacade.updateAudioMedia(
                    id = id,
                    name = name,
                    artist = artist,
                    description = description,
                    lyricFileNameWithExt = newLyricFileNameWithExt,
                )

                // 새 파일로 바뀐 경우에만 옛 파일을 정리한다. 파일 삭제 실패는 무시한다.
                if (oldLyricFileNameWithExt != null && oldLyricFileNameWithExt != newLyricFileNameWithExt) {
                    deleteLyricFile(oldLyricFileNameWithExt)
                }
            }
        }
    }

    override suspend fun importSharedAudio(
        contentUriString: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val copiedInfo = fileManager.copyAudioFromContentUri(contentUriString).getOrThrow()
                val source = "quickShare://sha256/${copiedInfo.sha256Hex}"

                // 중복 파일 확인: 같은 SHA-256이 이미 저장된 경우 복사본을 삭제하고 에러 반환
                suspendRunCatching { audioMediaDao.getDataBySource(source) }.onSuccess {
                    deleteAudioFile(copiedInfo.fileNameWithExt)
                    throw AlreadyDownloadedMedia()
                }

                val entity = AudioMediaEntity(
                    source = source,
                    name = copiedInfo.title ?: copiedInfo.fileNameWithExt,
                    artist = copiedInfo.artist ?: "",
                    description = "",
                    rawAudioDurationSeconds = copiedInfo.durationMs?.let { it / 1000 },
                    audioFileNameWithExt = copiedInfo.fileNameWithExt,
                    imageFileNameWithExt = copiedInfo.imageFileNameWithExt,
                ).clampText()
                val added = audioMediaFacade.addNewAudioMedia(
                    newAudioMedia = entity,
                    requestedPlaylistId = playlistIdOnDownload,
                    shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
                )
                added.toAddedAudioMedia(entity)
            }
        }
    }

    override suspend fun importUploadedAudio(
        uploadedFileFullPath: String,
        originalFileName: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia> {
        return withContext(Dispatchers.IO) {
            suspendRunCatching {
                val copiedInfo = fileManager.saveUploadedAudio(uploadedFileFullPath, originalFileName).getOrThrow()
                val source = "fileShare://sha256/${copiedInfo.sha256Hex}"

                suspendRunCatching { audioMediaDao.getDataBySource(source) }.onSuccess {
                    deleteAudioFile(copiedInfo.fileNameWithExt)
                    throw AlreadyDownloadedMedia()
                }

                val entity = AudioMediaEntity(
                    source = source,
                    name = copiedInfo.title ?: copiedInfo.fileNameWithExt,
                    artist = copiedInfo.artist ?: "",
                    description = "",
                    rawAudioDurationSeconds = copiedInfo.durationMs?.let { it / 1000 },
                    audioFileNameWithExt = copiedInfo.fileNameWithExt,
                    imageFileNameWithExt = copiedInfo.imageFileNameWithExt,
                ).clampText()
                val added = audioMediaFacade.addNewAudioMedia(
                    newAudioMedia = entity,
                    requestedPlaylistId = playlistIdOnDownload,
                    shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
                )
                added.toAddedAudioMedia(entity)
            }
        }
    }

    private fun AddNewAudioMediaResult.toAddedAudioMedia(entity: AudioMediaEntity): AddedAudioMedia {
        return AddedAudioMedia(
            audioMedia = entity.copy(id = audioMediaId).toDomain(fileManager).getOrThrow(),
            totalPlaylistMediaCount = totalPlaylist.audioMediaCount,
            totalPlaylistId = totalPlaylistId,
            resolvedPlaylistIdOnDownload = resolvedPlaylistId,
        )
    }

    override suspend fun deleteDownloadedFile(downloadedAudioMedia: DownloadedMedia): Result<Unit> {
       return suspendRunCatching {
           deleteAudioFile(downloadedAudioMedia.audioFileNameWithExt).getOrThrow()
           downloadedAudioMedia.imageFileNameWithExt?.let { deleteImageFile(it).getOrThrow() }
       }
    }

    override suspend fun deleteAudioMedia(id: Int): Result<Unit> = suspendRunCatching {
        withContext(Dispatchers.IO) {
            val data = audioMediaDao.getDataById(id)
            audioMediaFacade.deleteAudioMedia(id)

            // 파일 삭제 실패해도 에러를 발생시키지 않는다
            coroutineScope {
                launch { deleteAudioFile(data.audioFileNameWithExt) }
                // 편집본은 원본과 같이 지워야 한다
                data.editedAudioFileNameWithExt?.let {
                    launch {
                        deleteAudioFile(it)
                    }
                }
                data.imageFileNameWithExt?.let {
                    launch {
                        deleteImageFile(it)
                    }
                }
                data.customImageFileNameWithExt?.let {
                    launch {
                        deleteImageFile(it)
                    }
                }
            }
        }
    }

    override suspend fun deleteOrphanAudioMedia(): Result<List<AudioMedia>> = suspendRunCatching {
        withContext(Dispatchers.IO) {
            audioMediaFacade.deleteOrphanAudioMedia()
                .mapNotNull { entity -> entity.toDomain(fileManager).getOrNull() }
        }
    }

    override suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit> =
        fileManager.deleteFileAtFullPath(fileFullPath)

    override suspend fun listAudioAndImageFileFullPaths(): Result<List<String>> =
        fileManager.listAudioAndImageFileFullPaths()

    override suspend fun correctAudioFileExtensions(): Result<Int> = withContext(Dispatchers.IO) {
        suspendRunCatching { AudioFileExtensionCorrector(audioMediaDao, fileManager).correctAll() }
    }

    override suspend fun exportAudioMediaToDevice(
        audioFileFullPath: String,
        title: String,
        artist: String,
        targetList: List<AudioExportTarget>,
    ): Result<AudioMediaExportStatus> = fileManager.exportAudioToPublicStorage(
        sourceAudioFileFullPath = audioFileFullPath,
        title = title,
        artist = artist.takeIf { it.isNotBlank() },
        targetList = targetList,
    ).map { exported ->
        if (exported.alreadyExported) {
            AudioMediaExportStatus.ALREADY_EXPORTED
        } else {
            AudioMediaExportStatus.EXPORTED
        }
    }

    // 저장 파일명(WithExt)과 카테고리를 아는 호출부에서만 사용한다. 카테고리별 디렉토리로 경로를 해석해 삭제한다.
    private suspend fun deleteAudioFile(fileNameWithExt: String): Result<Unit> =
        fileManager.getAudioFileFullPath(fileNameWithExt).mapCatching {
            fileManager.deleteFileAtFullPath(it).getOrThrow()
        }

    private suspend fun deleteImageFile(fileNameWithExt: String): Result<Unit> =
        fileManager.getImageFileFullPath(fileNameWithExt).mapCatching {
            fileManager.deleteFileAtFullPath(it).getOrThrow()
        }

    private suspend fun deleteLyricFile(fileNameWithExt: String): Result<Unit> =
        fileManager.getLyricFileFullPath(fileNameWithExt).mapCatching {
            fileManager.deleteFileAtFullPath(it).getOrThrow()
        }
}
