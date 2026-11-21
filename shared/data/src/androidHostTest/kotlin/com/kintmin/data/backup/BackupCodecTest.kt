package com.kintmin.data.backup

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFileInvalidException
import com.kintmin.domain.backup.model.BackupVersionUnsupportedException
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationEditProject
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStep
import com.kintmin.domain.data_migration.model.MigrationTrack
import com.kintmin.domain.playlist.model.PlaylistType
import okio.Buffer
import okio.Sink
import okio.buffer
import okio.gzip
import okio.use
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BackupCodecTest {

    private fun audio(id: Int, description: String = "설명", lyric: String? = "$id.lrc") = MigrationAudioMedia(
        id = id, source = "https://youtu.be/video$id", name = "노래 제목 $id", artist = "가수 $id",
        description = description, rawAudioDurationSeconds = 240, audioFileName = "${uuid(id)}.m4a",
        imageFileName = "${uuid(id + 1_000_000)}.webp", lyricFileName = lyric, tjKaraokeNumber = "12345",
        editedAudioFileName = null, editedAudioDurationSeconds = null, createdTimeMillis = 1_700_000_000_000 + id,
    )

    private fun uuid(seed: Int) = Random(seed).let { r -> List(32) { "0123456789abcdef"[r.nextInt(16)] }.joinToString("") }

    private val content = BackupContent(
        snapshot = MigrationSnapshot(
            audioMedias = listOf(audio(1)),
            playlists = listOf(MigrationPlaylist(10, "드라이브", "설명", null, 0, PlaylistType.USER, 3)),
            tracks = listOf(MigrationTrack(10, 1, 0)),
            editProjects = listOf(MigrationEditProject(1, "{\"version\":1}")),
            steps = listOf(MigrationStep(1_000, 42)),
        ),
        downloadSettings = MigrationDownloadSettings(shouldInsertAtTop = true, playlistId = 10),
        lyrics = mapOf("1.lrc" to "[00:01.00]가사"),
        createdAtMillis = 123,
    )

    @Test
    fun `쓴 그대로 읽힌다`() {
        assertEquals(content, BackupCodec.decode(BackupCodec.encode(content)))
    }

    @Test
    fun `더 새 버전의 백업은 읽지 않는다`() {
        val newer = gzip("""{"backupVersion":${BackupCodec.FORMAT_VERSION + 1},"createdAtMillis":0,
            "snapshot":{"audioMedias":[],"playlists":[],"tracks":[],"editProjects":[],"steps":[]}}""")

        assertThrows(BackupVersionUnsupportedException::class.java) { BackupCodec.decode(newer) }
    }

    @Test
    fun `더 새 버전이 나머지 모양을 바꿨어도 손상이 아니라 버전이 높다고 알린다`() {
        val reshaped = gzip("""{"backupVersion":${BackupCodec.FORMAT_VERSION + 1},"library":{"songs":[]}}""")

        assertThrows(BackupVersionUnsupportedException::class.java) { BackupCodec.decode(reshaped) }
    }

    @Test
    fun `백업 파일이 아니면 읽을 수 없다고 알린다`() {
        assertThrows(BackupFileInvalidException::class.java) { BackupCodec.decode("hello".encodeToByteArray()) }
        assertThrows(BackupFileInvalidException::class.java) { BackupCodec.decode(gzip("{}")) }
    }

    @Test
    fun `5천 곡과 10년치 걸음수도 자동 백업 한도 안에 든다`() {
        val random = Random(7)
        // 한글 음절을 무작위로 엮은 낱말 2천 개. 실제 글보다 덜 반복돼 크기를 넉넉히 잡는다.
        val words = List(2_000) { String(CharArray(2 + random.nextInt(3)) { '가' + random.nextInt(11_172) }) } +
            listOf("https://youtu.be/", "#노래", "MV", "Official", "가사", "작사", "작곡", "편곡")
        fun text(chars: Int) = buildString { while (length < chars) append(words[random.nextInt(words.size)]).append(' ') }
        fun lyricText() = buildString {
            var second = 0
            while (length < 4_000) {
                append("[%02d:%02d.00]".format(second / 60, second % 60)).append(text(30)).append('\n')
                second += 4
            }
        }
        val songCount = 5_000
        val audios = (1..songCount).map { audio(it, description = text(1_500)) }
        val playlists = (1..200).map { MigrationPlaylist(it, "재생목록 $it", text(100), null, it, PlaylistType.USER, it.toLong()) }
        val tracks = audios.flatMap { listOf(MigrationTrack(1, it.id, it.id), MigrationTrack(1 + it.id % 200, it.id, it.id)) }
        var sensor = 0L
        val steps = (0 until 3_650 * 50).map { MigrationStep(1_600_000_000_000 + it * 1_800_000L, sensor.also { sensor += random.nextInt(300) }) }
        val large = BackupContent(
            snapshot = MigrationSnapshot(audios, playlists, tracks, emptyList(), steps),
            downloadSettings = null,
            lyrics = audios.associate { it.lyricFileName!! to lyricText() },
            createdAtMillis = 0,
        )

        val size = BackupCodec.encode(large).size
        println("백업 크기: ${size / 1024} KB (한도 ${BackupFiles.LOCAL_COPY_MAX_BYTES / 1024} KB)")

        assertTrue("백업 ${size}B", size < BackupFiles.LOCAL_COPY_MAX_BYTES)
    }

    private fun gzip(text: String): ByteArray {
        val buffer = Buffer()
        (buffer as Sink).gzip().buffer().use { it.writeUtf8(text) }
        return buffer.readByteArray()
    }
}
