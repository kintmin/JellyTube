package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 편집본을 만든 편집 상태(타임라인). 음원 하나에 최대 하나다(1:1).
 *
 * 다시 편집하러 들어왔을 때 이전 조각 배치를 그대로 이어서 고칠 수 있게 남겨 둔다.
 *
 * AUDIO_MEDIA에 컬럼으로 두지 않고 별도 테이블로 뺀 이유:
 * - 목록·재생 화면은 `SELECT * FROM AUDIO_MEDIA`로 음원을 통째로 읽는다. 편집 화면에서만 쓰는
 *   타임라인을 같은 행에 두면 편집하지 않는 화면에서도 매번 같이 읽힌다.
 * - 편집본 파일명·길이(AUDIO_MEDIA.editedAudio…)는 재생·목록 표시에 늘 필요해 AUDIO_MEDIA에 남긴다.
 *
 * 파일이 아니라 DB에 JSON으로 두는 이유:
 * - 수백 바이트~수 KB로 작고, 항상 통째로 읽고 쓰며, 필드 단위로 조회할 일이 없다.
 * - 편집본 적용·해제와 같은 트랜잭션으로 묶여 서로 어긋나지 않는다. 파일이면 쓰기와 DB 갱신이 따로라
 *   중간에 실패하면 어긋나고, 고아 파일 정리도 따로 필요하다.
 * - 음원이 지워지면 외래키 CASCADE로 같이 지워진다.
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
    /** 편집 타임라인 JSON. 형식은 local_db/mapper/AudioEditProjectMapper 참고. */
    val timelineJson: String,
)
