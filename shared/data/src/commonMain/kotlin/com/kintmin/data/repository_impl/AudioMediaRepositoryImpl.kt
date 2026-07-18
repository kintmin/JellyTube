package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao_facade.AddNewAudioMediaResult
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.lyrics.model.LyricsVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override suspend fun downloadAudioMedia(downloadUrl: String): Result<DownloadedMedia> {
        return withContext(Dispatchers.IO) {
            runCatching {

                val fileName = Uuid.random().toString()

                val audioDownloadBasePath = fileManager.getAudioDownloadBasePath(
                    fileName = fileName,
                ).getOrThrow()

                val downloadDto = pythonExecutor.downloadYoutubeMedia(
                    youtubeUrl = downloadUrl,
                    audioDownloadPath = audioDownloadBasePath,
                ).getOrThrow()

                val audioFileNameWithExt = downloadDto.audioFileNameWithExt.ifBlank {
                    throw Exception("다운로드한 오디오 파일명을 확인할 수 없습니다.")
                }

                val imageFileExt = httpDataSource.downloadImage(
                    imageUrl = downloadDto.thumbnailDownloadUrl
                ).getOrNull()?.let { image ->
                    fileManager.saveImageWithCompression(
                        imageData = image,
                        fileName = fileName,
                    ).getOrNull()
                }

                DownloadedMedia(
                    downloadUrl = downloadUrl,
                    title = downloadDto.title,
                    audioFileNameWithExt = audioFileNameWithExt,
                    imageFileNameWithExt = imageFileExt?.let { "${fileName}.${it}" },
                    duration = downloadDto.duration,
                    uploader = downloadDto.uploader,
                    description = downloadDto.description,
                )
            }
        }
    }

    override suspend fun addAudioMedia(
        downloadedAudioMedia: DownloadedMedia,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val audioMediaEntityToSave = AudioMediaEntity(
                    source = downloadedAudioMedia.downloadUrl,
                    name = downloadedAudioMedia.title,
                    artist = downloadedAudioMedia.uploader,
                    description = downloadedAudioMedia.description,
                    rawAudioDurationSeconds = downloadedAudioMedia.duration.toLongOrNull(),
                    audioFileNameWithExt = downloadedAudioMedia.audioFileNameWithExt,
                    imageFileNameWithExt = downloadedAudioMedia.imageFileNameWithExt,
                )
                val added = audioMediaFacade.addNewAudioMedia(
                    newAudioMedia = audioMediaEntityToSave,
                    requestedPlaylistId = playlistIdOnDownload,
                    shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
                )
                added.toAddedAudioMedia(audioMediaEntityToSave)
            }
        }
    }

    override suspend fun getAudioMediaBySource(source: String): Result<AudioMedia> {
        return withContext(Dispatchers.IO) {
            runCatching {
                audioMediaDao.getDataBySource(source).toDomain(fileManager).getOrThrow()
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

    override suspend fun saveImage(imageData: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val fileName = Uuid.random().toString()
                val ext = fileManager.saveImageWithCompression(imageData, fileName).getOrThrow()
                fileManager.getImageFileFullPath(fileNameWithExt = "$fileName.$ext").getOrThrow()
            }
        }
    }

    override suspend fun updateKaraokeNumber(id: Int, tjKaraokeNumber: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                audioMediaDao.updateKaraokeNumber(id, tjKaraokeNumber)
            }
        }
    }

    override suspend fun clearKaraokeNumber(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                audioMediaDao.clearKaraokeNumber(id)
            }
        }
    }

    override suspend fun saveLyrics(text: String, synced: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val fileName = Uuid.random().toString()
                val ext = fileManager.saveLyrics(text, fileName, synced).getOrThrow()
                fileManager.getLyricFileFullPath(fileNameWithExt = "$fileName.$ext").getOrThrow()
            }
        }
    }

    override suspend fun getLyrics(lyricFileFullPath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
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
            runCatching {
                val (baseName, extName) = splitLyricFileName(baseLyricFileFullPath)
                val synced = extName.equals("lrc", ignoreCase = true)
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
            runCatching {
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
            runCatching {
                val (baseName, extName) = splitLyricFileName(baseLyricFileFullPath)
                val variantFileNameWithExt = "$baseName.${variant.infix()}.$extName"
                deleteLyricFile(variantFileNameWithExt).getOrThrow()
                Unit
            }
        }
    }

    private fun splitLyricFileName(baseLyricFileFullPath: String): Pair<String, String> {
        val nameWithExt = fileManager.getFileNameWithExt(baseLyricFileFullPath).getOrThrow()
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
            runCatching {
                val oldLyricFileNameWithExt = audioMediaDao.getDataById(id).lyricFileNameWithExt
                audioMediaDao.clearLyricFile(id)
                oldLyricFileNameWithExt?.let { deleteLyricFile(it) }
                Unit
            }
        }
    }

    override suspend fun updateAudioMedia(
        id: Int,
        name: String?,
        artist: String?,
        description: String?,
        imageFileFullPath: String?,
        lyricFileFullPath: String?,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val newImageFileNameWithExt = imageFileFullPath?.let {
                    fileManager.getFileNameWithExt(it).getOrThrow()
                }
                val newLyricFileNameWithExt = lyricFileFullPath?.let {
                    fileManager.getFileNameWithExt(it).getOrThrow()
                }
                // 이미지/가사를 교체하는 경우, 교체 후 orphan이 될 옛 파일명을 미리 확보한다.
                val oldEntity = if (newImageFileNameWithExt != null || newLyricFileNameWithExt != null) {
                    audioMediaDao.getDataById(id)
                } else null
                val oldImageFileNameWithExt = newImageFileNameWithExt?.let { oldEntity?.imageFileNameWithExt }
                val oldLyricFileNameWithExt = newLyricFileNameWithExt?.let { oldEntity?.lyricFileNameWithExt }

                audioMediaFacade.updateAudioMedia(
                    id = id,
                    name = name,
                    artist = artist,
                    description = description,
                    imageFileNameWithExt = newImageFileNameWithExt,
                    lyricFileNameWithExt = newLyricFileNameWithExt,
                )

                // 새 파일로 바뀐 경우에만 옛 파일을 정리한다. 파일 삭제 실패는 무시한다.
                if (oldImageFileNameWithExt != null && oldImageFileNameWithExt != newImageFileNameWithExt) {
                    deleteImageFile(oldImageFileNameWithExt)
                }
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
            runCatching {
                val copiedInfo = fileManager.copyAudioFromContentUri(contentUriString).getOrThrow()
                val source = "quickShare://sha256/${copiedInfo.sha256Hex}"

                // 중복 파일 확인: 같은 SHA-256이 이미 저장된 경우 복사본을 삭제하고 에러 반환
                runCatching { audioMediaDao.getDataBySource(source) }.onSuccess {
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
                )
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
        bytes: ByteArray,
        originalFileName: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val copiedInfo = fileManager.saveUploadedAudio(bytes, originalFileName).getOrThrow()
                val source = "fileShare://sha256/${copiedInfo.sha256Hex}"

                runCatching { audioMediaDao.getDataBySource(source) }.onSuccess {
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
                )
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
       return runCatching {
           deleteAudioFile(downloadedAudioMedia.audioFileNameWithExt).getOrThrow()
           downloadedAudioMedia.imageFileNameWithExt?.let { deleteImageFile(it).getOrThrow() }
       }
    }

    override suspend fun deleteAudioMedia(id: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val data = audioMediaDao.getDataById(id)
            audioMediaFacade.deleteAudioMedia(id)

            // 파일 삭제 실패해도 에러를 발생시키지 않는다
            coroutineScope {
                launch { deleteAudioFile(data.audioFileNameWithExt) }
                data.imageFileNameWithExt?.let {
                    launch {
                        deleteImageFile(it)
                    }
                }
            }
        }
    }

    override suspend fun deleteOrphanAudioMedia(): Result<List<AudioMedia>> = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaFacade.deleteOrphanAudioMedia()
                .mapNotNull { entity -> entity.toDomain(fileManager).getOrNull() }
        }
    }

    override suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit> =
        fileManager.deleteFileAtFullPath(fileFullPath)

    override suspend fun listAudioAndImageFileFullPaths(): Result<List<String>> =
        fileManager.listAudioAndImageFileFullPaths()

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
