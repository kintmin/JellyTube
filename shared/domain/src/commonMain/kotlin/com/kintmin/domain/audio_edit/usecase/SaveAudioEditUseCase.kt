package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.SavedAudioEdit

/** [newMediaName] 이 없으면 원본에 덮어쓰고, 있으면 그 이름의 새 음원으로 저장한다. */
class SaveAudioEditUseCase constructor(
    private val applyAudioEditUseCase: ApplyAudioEditUseCase,
    private val saveEditedAudioAsNewMediaUseCase: SaveEditedAudioAsNewMediaUseCase,
) {
    suspend operator fun invoke(audioMediaId: Int, timeline: AudioEditTimeline, newMediaName: String?): Result<SavedAudioEdit> =
        if (newMediaName == null) {
            applyAudioEditUseCase(audioMediaId = audioMediaId, timeline = timeline).map(SavedAudioEdit::Overwritten)
        } else {
            saveEditedAudioAsNewMediaUseCase(audioMediaId = audioMediaId, parts = timeline.toParts(), name = newMediaName)
                .map(SavedAudioEdit::SavedAsNew)
        }
}
