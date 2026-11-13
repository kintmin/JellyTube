package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.presentation.port.model.toMediaControlData

/** 저장에 성공한 뒤의 처리. 돌려준 알림을 띄우면 된다. */
fun MediaControllerManager.completeAudioEditSave(
    audioMedia: AudioMedia,
    isOverwrite: Boolean,
): WorkResultNotice.AudioEditSave {
    if (!isOverwrite) return WorkResultNotice.AudioEditSave.NewMediaSuccess(audioMedia.name, audioMedia.id)

    tryReplaceMediaItem(mediaId = audioMedia.id, mediaItem = audioMedia.toMediaControlData())
    return WorkResultNotice.AudioEditSave.OverwriteSuccess(audioMedia.name, audioMedia.id)
}
