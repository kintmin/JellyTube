package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationSnapshot

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

    /**
     * 보내는 기기에 실제로 없는 파일을 가리키는 값을 지운다. 음원 파일이 없는 곡은 통째로 뺀다 —
     * 재생할 수 없는 곡을 들여오지 않는다. 그 곡의 소속은 계획에서 저절로 빠진다.
     */
    fun keepAvailable(snapshot: MigrationSnapshot, available: Set<MigrationFile>): MigrationSnapshot {
        fun String?.ifAvailable(kind: MigrationFileKind) = this?.takeIf { MigrationFile(kind, it) in available }

        return snapshot.copy(
            audioMedias = snapshot.audioMedias
                .filter { MigrationFile(MigrationFileKind.AUDIO, it.audioFileName) in available }
                .map { audio ->
                    val editedAudioFileName = audio.editedAudioFileName.ifAvailable(MigrationFileKind.AUDIO)
                    audio.copy(
                        imageFileName = audio.imageFileName.ifAvailable(MigrationFileKind.IMAGE),
                        lyricFileName = audio.lyricFileName.ifAvailable(MigrationFileKind.LYRIC),
                        editedAudioFileName = editedAudioFileName,
                        editedAudioDurationSeconds = audio.editedAudioDurationSeconds.takeIf { editedAudioFileName != null },
                    )
                },
            playlists = snapshot.playlists.map { playlist ->
                playlist.copy(customImageFileName = playlist.customImageFileName.ifAvailable(MigrationFileKind.IMAGE))
            },
        )
    }

    /** 받는 기기는 이 이름으로 파일을 만든다. 보내는 쪽이 준 이름이 디렉터리를 벗어나지 못하게 한다. */
    fun isSafeName(name: String): Boolean =
        name.isNotEmpty() && name != "." && name != ".." && name.none { it == '/' || it == '\\' || it == '\u0000' }
}
