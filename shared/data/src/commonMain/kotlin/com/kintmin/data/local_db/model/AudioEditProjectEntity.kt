package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 편집본을 만든 편집 상태(타임라인). 음원 하나에 최대 하나다(1:1).
 *
 * 다시 편집하러 들어왔을 때 이전 조각 배치를 그대로 이어서 고칠 수 있게 남겨 둔다.
 */
@Entity(
    tableName = "AUDIO_EDIT_PROJECT",
    foreignKeys = [
        ForeignKey(
            entity = AudioMediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioMediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AudioEditProjectEntity(
    @PrimaryKey val audioMediaId: Int,
    val timelineJson: String,
)
