package com.kintmin.domain.audio_edit.model

import com.kintmin.domain.audio_media.model.AudioMedia

sealed interface SavedAudioEdit {
    val audioMedia: AudioMedia

    /** 원본 자리에 편집본을 붙였다. 재생 큐에 있던 같은 곡은 바꿔 줘야 편집본이 재생된다. */
    data class Overwritten(override val audioMedia: AudioMedia) : SavedAudioEdit

    data class SavedAsNew(override val audioMedia: AudioMedia) : SavedAudioEdit
}
