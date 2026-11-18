package com.kintmin.data.local_file

import com.kintmin.data.local_file.model.CopiedAudioInfo
import com.kintmin.data.local_file.model.ExportedAudioInfo
import com.kintmin.domain.audio_media.model.AudioExportTarget
import okio.FileSystem
import okio.Path

/** 음원·이미지·가사·임시 공간 경로만 [root] 아래로 돌린다. 나머지는 테스트에서 쓰지 않는다. */
internal class DirectoryFileManager(private val root: Path) : FileManager {
    private fun dir(name: String): Path = (root / name).also { FileSystem.SYSTEM.createDirectories(it) }

    override fun getAudioFileFullPath(fileNameWithExt: String) = runCatching { (dir("audios") / fileNameWithExt).toString() }
    override fun getImageFileFullPath(fileNameWithExt: String) = runCatching { (dir("images") / fileNameWithExt).toString() }
    override fun getLyricFileFullPath(fileNameWithExt: String) = runCatching { (dir("lyrics") / fileNameWithExt).toString() }
    override fun getMigrationStagingDirPath() = runCatching { dir("staging").toString() }

    override fun getFileNameWithExt(fileFullPath: String): Result<String> = TODO()
    override fun getAudioDownloadBasePath(fileName: String): Result<String> = TODO()
    override suspend fun saveImageWithCompression(imageData: ByteArray, fileName: String): Result<String> = TODO()
    override suspend fun copyImageFile(sourceFileNameWithExt: String, targetFileName: String): Result<String> = TODO()
    override suspend fun saveLyrics(text: String, fileName: String, synced: Boolean): Result<String> = TODO()
    override suspend fun fetchLyrics(fileNameWithExt: String): Result<String> = TODO()
    override suspend fun copyAudioFromContentUri(contentUriString: String): Result<CopiedAudioInfo> = TODO()
    override suspend fun saveUploadedAudio(sourceFileFullPath: String, originalFileName: String): Result<CopiedAudioInfo> = TODO()
    override suspend fun exportAudioToPublicStorage(
        sourceAudioFileFullPath: String,
        title: String,
        artist: String?,
        targetList: List<AudioExportTarget>,
    ): Result<ExportedAudioInfo> = TODO()
    override suspend fun deleteFileAtFullPath(fileFullPath: String): Result<Unit> = TODO()
    override suspend fun listAudioAndImageFileFullPaths(): Result<List<String>> = TODO()
    override fun clearDiskCache(): Result<Unit> = TODO()
    override suspend fun appendAppLog(date: String, line: String): Result<Unit> = TODO()
    override suspend fun fetchAppLogDateList(): Result<List<String>> = TODO()
    override suspend fun fetchAppLogLineList(date: String): Result<List<String>> = TODO()
    override suspend fun fetchAppLogFileFullPaths(): Result<List<String>> = TODO()
}
