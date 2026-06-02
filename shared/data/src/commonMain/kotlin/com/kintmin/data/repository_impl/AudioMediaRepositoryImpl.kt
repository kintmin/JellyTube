package com.kintmin.data.repository_impl

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.mapper.toDomain
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.model.Ext
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
                val fileName = UUID.randomUUID().toString()

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
        playlistIdOnDownload: Int,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<Pair<AudioMedia, Int>> {
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
                val (newAudioMediaId, totalPlaylist) = audioMediaFacade.addNewAudioMedia(
                    newAudioMedia = audioMediaEntityToSave,
                    playlistIdOnDownload = playlistIdOnDownload,
                    shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
                )
                audioMediaEntityToSave.copy(id = newAudioMediaId).toDomain(fileManager).getOrThrow() to totalPlaylist.audioMediaCount
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
                val fileName = UUID.randomUUID().toString()
                val ext = fileManager.saveImageWithCompression(imageData, fileName).getOrThrow()
                fileManager.getFullPathWithExt(fileName = fileName, ext = ext).getOrThrow()
            }
        }
    }

    override suspend fun updateAudioMedia(
        id: Int,
        name: String?,
        artist: String?,
        description: String?,
        imageFileFullPath: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                audioMediaDao.updateAudioMedia(
                    id = id,
                    name = name,
                    artist = artist,
                    description = description,
                    imageFileNameWithExt = imageFileFullPath?.let {
                        fileManager.getFileNameWithExt(it).getOrThrow()
                    },
                )
            }
        }
    }

    override suspend fun importSharedAudio(
        contentUriString: String,
        playlistIdOnDownload: Int,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<Pair<AudioMedia, Int>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val copiedInfo = fileManager.copyAudioFromContentUri(contentUriString).getOrThrow()
                val source = "quickShare://sha256/${copiedInfo.sha256Hex}"

                // Ï§ëÎ≥µ ?åÏùº ?ïÏù∏: Í∞ôÏ? SHA-256???¥Î? ?Ä?•Îêú Í≤ΩÏö∞ Î≥µÏÇ¨Î≥∏ÏùÑ ??†ú?òÍ≥† ?êÎü¨ Î∞òÌôò
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
                val (newId, totalPlaylist) = audioMediaFacade.addNewAudioMedia(
                    newAudioMedia = entity,
                    playlistIdOnDownload = playlistIdOnDownload,
                    shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
                )
                entity.copy(id = newId).toDomain(fileManager).getOrThrow() to totalPlaylist.audioMediaCount
            }
        }
    }

    override suspend fun importUploadedAudio(
        bytes: ByteArray,
        originalFileName: String,
        playlistIdOnDownload: Int,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<Pair<AudioMedia, Int>> {
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
                val (newId, totalPlaylist) = audioMediaFacade.addNewAudioMedia(
                    newAudioMedia = entity,
                    playlistIdOnDownload = playlistIdOnDownload,
                    shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
                )
                entity.copy(id = newId).toDomain(fileManager).getOrThrow() to totalPlaylist.audioMediaCount
            }
        }
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

            // ?åÏùº ??†ú???§Ìå®?¥ÎèÑ ?êÎü¨Î•?Î∞úÏÉù?úÌÇ§ÏßÄ ?äÎäî??
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
}
