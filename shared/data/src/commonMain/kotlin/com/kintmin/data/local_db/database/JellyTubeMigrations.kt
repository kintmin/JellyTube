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
