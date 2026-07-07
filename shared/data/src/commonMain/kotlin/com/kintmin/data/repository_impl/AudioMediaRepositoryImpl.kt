package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao_facade.AddNewAudioMediaResult
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
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

                val audioFileFullPath = fileManager.getFullPathWithExt(
                    fileName = fileName,
                    ext = Ext.MP3,
                ).getOrThrow()

                val downloadDto = pythonExecutor.downloadYoutubeMedia(
                    youtubeUrl = downloadUrl,
                    audioDownloadPath = audioFileFullPath,
                ).getOrThrow()

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
                    audioFileNameWithExt = "${fileName}.${Ext.MP3}",
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
                fileManager.getFullPathWithExt(fileName = fileName, ext = ext).getOrThrow()
            }
        }
    }

    override suspend fun saveLyrics(text: String, synced: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val fileName = Uuid.random().toString()
                val ext = fileManager.saveLyrics(text, fileName, synced).getOrThrow()
                fileManager.getFullPathWithExt(fileName = fileName, ext = ext).getOrThrow()
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

    override suspend fun deleteLyrics(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val oldLyricFileNameWithExt = audioMediaDao.getDataById(id).lyricFileNameWithExt
                audioMediaDao.clearLyricFile(id)
                oldLyricFileNameWithExt?.let { fileManager.deleteFile(it) }
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
                    fileManager.deleteFile(oldImageFileNameWithExt)
                }
                if (oldLyricFileNameWithExt != null && oldLyricFileNameWithExt != newLyricFileNameWithExt) {
                    fileManager.deleteFile(oldLyricFileNameWithExt)
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
                    fileManager.deleteFile(copiedInfo.fileNameWithExt)
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
                    fileManager.deleteFile(copiedInfo.fileNameWithExt)
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
           fileManager.deleteFile(downloadedAudioMedia.audioFileNameWithExt).getOrThrow()
           downloadedAudioMedia.imageFileNameWithExt?.let { fileManager.deleteFile(it).getOrThrow() }
       }
    }

    override suspend fun deleteAudioMedia(id: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val data = audioMediaDao.getDataById(id)
            audioMediaFacade.deleteAudioMedia(id)

            // 파일 삭제 실패해도 에러를 발생시키지 않는다
            coroutineScope {
                launch { fileManager.deleteFile(data.audioFileNameWithExt) }
                data.imageFileNameWithExt?.let {
                    launch {
                        fileManager.deleteFile(it)
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

    override suspend fun deleteFile(fileNameWithExt: String): Result<Unit> =
        fileManager.deleteFile(fileNameWithExt)

    override suspend fun listAudioAndImageFileNames(): Result<List<String>> =
        fileManager.listAudioAndImageFileNames()
}
