package com.kintmin.data.local_db.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.kintmin.domain.playlist.model.PlaylistType

/**
 * v3 → v4: PLAYLIST에 type 컬럼 추가.
 * v3 이하 DB는 과거 콜백이 전체=id 1, 미분류=id 2로 고정 시딩했으므로 그 id를 기준으로 backfill 한다.
 * (v4부터는 고정 id 없이 type으로만 구분한다.)
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE PLAYLIST ADD COLUMN type TEXT NOT NULL DEFAULT '${PlaylistType.USER.name}'")
        connection.execSQL("UPDATE PLAYLIST SET type = '${PlaylistType.TOTAL.name}' WHERE id = 1")
        connection.execSQL("UPDATE PLAYLIST SET type = '${PlaylistType.UNCATEGORIZED.name}' WHERE id = 2")
    }
}

/**
 * v8 → v9: 비밀 곡을 "전체" 에서 "비밀 전체" 로 옮긴다. 스키마는 그대로이고 데이터만 움직인다 —
 * 전까지는 비밀 곡이 "전체" 에 담긴 채 **읽을 때** 걸러졌다.
 *
 * `sequence` 는 그대로 가져간다. "전체" 안에서 유일했으므로 옮긴 뒤에도 겹치지 않고 순서가 남는다.
 * 옮긴 두 플레이리스트의 곡 수·재생시간·커버는 누적값이라 여기서 다시 센다.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        MIGRATION_8_9_STATEMENTS.forEach { connection.execSQL(it) }
    }
}

/** 문장을 따로 두어 테스트가 **실제로 도는 SQL** 을 그대로 실행한다. */
internal val MIGRATION_8_9_STATEMENTS: List<String> = buildList {
    val secretTotal = PlaylistType.SECRET_TOTAL.name
    val total = PlaylistType.TOTAL.name
    val secret = PlaylistType.SECRET.name

    add(
        """
INSERT INTO PLAYLIST (name, description, audioMediaCount, rawPlayTimeDuration, rawCreatedTime, imageFileNameWithExt, isCustomImage, sequence, type)
SELECT '${PlaylistType.SECRET_TOTAL.defaultName}', '', 0, 0, CAST(strftime('%s', 'now') AS INTEGER) * 1000, NULL, 0, 0, '$secretTotal'
WHERE NOT EXISTS (SELECT 1 FROM PLAYLIST WHERE type = '$secretTotal')
""",
    )
    add(
        """
UPDATE PLAYLIST_TRACK
SET playlistId = (SELECT id FROM PLAYLIST WHERE type = '$secretTotal' LIMIT 1)
WHERE playlistId IN (SELECT id FROM PLAYLIST WHERE type = '$total')
  AND audioMediaId IN (
    SELECT SPT.audioMediaId FROM PLAYLIST_TRACK SPT
    JOIN PLAYLIST SP ON SP.id = SPT.playlistId
    WHERE SP.type = '$secret'
  )
""",
    )
    add(
        """
UPDATE PLAYLIST SET
    audioMediaCount = (SELECT COUNT(*) FROM PLAYLIST_TRACK PT WHERE PT.playlistId = PLAYLIST.id),
    rawPlayTimeDuration = (
        SELECT COALESCE(SUM(COALESCE(AM.editedAudioDurationSeconds, AM.rawAudioDurationSeconds, 0)), 0)
        FROM PLAYLIST_TRACK PT JOIN AUDIO_MEDIA AM ON AM.id = PT.audioMediaId
        WHERE PT.playlistId = PLAYLIST.id
    ),
    imageFileNameWithExt = CASE WHEN isCustomImage THEN imageFileNameWithExt ELSE (
        SELECT COALESCE(AM.customImageFileNameWithExt, AM.imageFileNameWithExt)
        FROM PLAYLIST_TRACK PT JOIN AUDIO_MEDIA AM ON AM.id = PT.audioMediaId
        WHERE PT.playlistId = PLAYLIST.id ORDER BY PT.sequence LIMIT 1
    ) END
WHERE type IN ('$total', '$secretTotal')
""",
    )
}
