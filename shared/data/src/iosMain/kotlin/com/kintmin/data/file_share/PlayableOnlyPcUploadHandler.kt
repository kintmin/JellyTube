package com.kintmin.data.file_share

import com.kintmin.data.data_migration.isPlayableAudioExtension
import com.kintmin.domain.audio_media.model.InvalidUploadedFileNameException
import com.kintmin.domain.file_share.PcUploadHandler

/** 이 기기가 재생하지 못하는 형식은 받기 전에 거절한다. 받아 두면 목록에는 보이는데 재생되지 않는다. */
internal class PlayableOnlyPcUploadHandler(
    private val delegate: PcUploadHandler,
) : PcUploadHandler by delegate {

    override fun validateFileName(rawFileName: String?): Result<String> =
        delegate.validateFileName(rawFileName).mapCatching { fileName ->
            if (!isPlayableAudioExtension(fileName.substringAfterLast('.', ""))) {
                throw InvalidUploadedFileNameException(UNPLAYABLE_MESSAGE)
            }
            fileName
        }

    private companion object {
        const val UNPLAYABLE_MESSAGE = "이 기기에서 재생할 수 없는 형식입니다."
    }
}
