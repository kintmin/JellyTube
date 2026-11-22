package com.kintmin.data.file_share

import com.kintmin.domain.audio_media.model.UploadedAudioFile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/** 받기만 하고 아직 목록에 넣지 않은 임시 파일. */
internal class StagedUploads {
    private val lock = SynchronizedObject()
    private val uploads = mutableMapOf<String, UploadedAudioFile>()

    fun put(stageId: String, upload: UploadedAudioFile) {
        synchronized(lock) { uploads[stageId] = upload }
    }

    fun take(stageId: String): UploadedAudioFile? = synchronized(lock) { uploads.remove(stageId) }

    fun clear() {
        val remaining = synchronized(lock) { uploads.values.toList().also { uploads.clear() } }
        remaining.forEach { deleteTempFile(it.fileFullPath) }
    }
}

internal fun deleteTempFile(path: String) {
    runCatching { SystemFileSystem.delete(Path(path), mustExist = false) }
}
