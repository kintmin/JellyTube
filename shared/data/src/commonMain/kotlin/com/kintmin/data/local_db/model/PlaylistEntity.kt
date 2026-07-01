package com.kintmin.data.local_db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

/**
 * 기본값 설정 시 DB에 addCallback 설정 필요
 */
@Entity(tableName = "PLAYLIST")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val audioMediaCount: Int,
    val rawPlayTimeDuration: Long,
    val rawCreatedTime: Long = Clock.System.now().toEpochMilliseconds(),
    val imageFileNameWithExt: String? = null,
    val isCustomImage: Boolean,
    // 0 = base playlist(전체/미분류)와 "아직 순서를 바꾼 적 없는" 항목의 기본값.
    // 사용자가 순서를 변경하면 그 대상 항목들에 1부터 재부여되어 base(0) 뒤로 정렬된다.
    @ColumnInfo(defaultValue = "0") val sequence: Int = 0,
)
