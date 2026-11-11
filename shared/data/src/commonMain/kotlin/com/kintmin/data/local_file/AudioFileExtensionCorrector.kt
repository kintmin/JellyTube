package com.kintmin.data.local_file

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.domain.audio_media.format.AudioContainerSniffer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * 음원 파일 확장자를 내용에 맞춘다. 원본과 편집본 모두 본다.
 *
 * 파일 이름을 먼저 바꾸고 DB 를 고친다. DB 갱신이 실패하면 이름을 되돌려 행과 파일이 어긋나지 않게 한다.
 */
internal class AudioFileExtensionCorrector(
    private val audioMediaDao: AudioMediaDao,
    private val fileManager: FileManager,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {

    /** 바로잡은 파일 수. */
    suspend fun correctAll(): Int = audioMediaDao.getAll().sumOf { entity ->
        val original = correct(entity.audioFileNameWithExt) { audioMediaDao.updateAudioFileName(entity.id, it) }
        val edited = entity.editedAudioFileNameWithExt
            ?.let { name -> correct(name) { audioMediaDao.updateEditedAudioFileName(entity.id, it) } }
            ?: false
        listOf(original, edited).count { it }
    }

    private suspend fun correct(fileName: String, updateFileName: suspend (String) -> Unit): Boolean {
        val source = fileManager.getAudioFileFullPath(fileName).getOrThrow().toPath()
        val header = readHeader(source) ?: return false
        val correctedName = AudioContainerSniffer.correctedFileName(fileName, header) ?: return false
        val destination = fileManager.getAudioFileFullPath(correctedName).getOrThrow().toPath()
        // 이름이 UUID 라 겹칠 일이 없지만, 겹치면 남의 파일을 덮지 않도록 건너뛴다.
        if (fileSystem.exists(destination)) return false

        fileSystem.atomicMove(source, destination)
        try {
            updateFileName(correctedName)
        } catch (e: Throwable) {
            fileSystem.atomicMove(destination, source)
            throw e
        }
        return true
    }

    private fun readHeader(path: Path): ByteArray? {
        if (!fileSystem.exists(path)) return null
        return fileSystem.source(path).buffer().use { source ->
            val header = ByteArray(AudioContainerSniffer.HEADER_SIZE)
            var read = 0
            while (read < header.size) {
                val count = source.read(header, read, header.size - read)
                if (count == -1) break
                read += count
            }
            header.copyOf(read)
        }
    }
}
