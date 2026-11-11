package com.kintmin.data.local_file

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.domain.audio_media.format.AudioContainer
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
        val correctedName = renameToMatchContent(fileName) ?: return false
        try {
            updateFileName(correctedName)
        } catch (e: Throwable) {
            fileSystem.atomicMove(audioPath(correctedName), audioPath(fileName))
            throw e
        }
        return true
    }

    /** 행이 없는 파일의 이름만 내용에 맞춘다. 바꿨으면 새 이름, 그대로면 null. */
    fun renameToMatchContent(fileName: String): String? {
        val source = audioPath(fileName)
        val container = detectContainer(source) ?: return null
        val correctedName = AudioContainerSniffer.correctedFileName(fileName, container) ?: return null
        val destination = audioPath(correctedName)
        // 이름이 UUID 라 겹칠 일이 없지만, 겹치면 남의 파일을 덮지 않도록 건너뛴다.
        if (fileSystem.exists(destination)) return null
        fileSystem.atomicMove(source, destination)
        return correctedName
    }

    private fun audioPath(fileName: String): Path = fileManager.getAudioFileFullPath(fileName).getOrThrow().toPath()

    /** 모르는 형식이면 null. 이름을 바꾸지 않는다. */
    private fun detectContainer(path: Path): AudioContainer? {
        if (!fileSystem.exists(path)) return null
        val header = read(path, offset = 0, size = AudioContainerSniffer.HEADER_SIZE)
        AudioContainerSniffer.detect(header)?.let { return it }
        // 앨범 아트가 든 ID3 태그는 수백 KB 라 앞부분에 첫 프레임이 없다.
        val payloadOffset = AudioContainerSniffer.id3PayloadOffset(header) ?: return null
        return AudioContainerSniffer.detectFrame(read(path, payloadOffset, AudioContainerSniffer.FRAME_HEADER_SIZE))
    }

    private fun read(path: Path, offset: Long, size: Int): ByteArray =
        fileSystem.source(path).buffer().use { source ->
            if (!source.request(offset)) return@use ByteArray(0)
            source.skip(offset)
            val bytes = ByteArray(size)
            var read = 0
            while (read < size) {
                val count = source.read(bytes, read, size - read)
                if (count == -1) break
                read += count
            }
            bytes.copyOf(read)
        }
}
