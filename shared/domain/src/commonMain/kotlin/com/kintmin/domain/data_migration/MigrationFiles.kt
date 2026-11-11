package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPlaylist

object MigrationFiles {

    /** 행이 가리키는 파일. 플레이리스트는 커스텀 이미지만 — 대표 이미지는 첫 곡에서 다시 정해진다. */
    fun referencedBy(
        audioMedias: List<MigrationAudioMedia>,
        playlists: List<MigrationPlaylist>,
    ): Set<MigrationFile> = buildSet {
        audioMedias.forEach { audio ->
            add(MigrationFile(MigrationFileKind.AUDIO, audio.audioFileName))
            audio.editedAudioFileName?.let { add(MigrationFile(MigrationFileKind.AUDIO, it)) }
            audio.imageFileName?.let { add(MigrationFile(MigrationFileKind.IMAGE, it)) }
            audio.lyricFileName?.let { add(MigrationFile(MigrationFileKind.LYRIC, it)) }
        }
        playlists.forEach { playlist ->
            playlist.customImageFileName?.let { add(MigrationFile(MigrationFileKind.IMAGE, it)) }
        }
    }
}
