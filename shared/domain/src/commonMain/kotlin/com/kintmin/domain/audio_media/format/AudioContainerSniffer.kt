package com.kintmin.domain.audio_media.format

/** 파일 앞부분만 보고 컨테이너를 가른다. 디코딩하지 않는다. 모르면 null. */
object AudioContainerSniffer {

    /** 판별에 필요한 앞부분 길이. Ogg Opus 의 `OpusHead` 가 28바이트째에 있다. */
    const val HEADER_SIZE = 64

    /** [id3PayloadOffset] 위치에서 읽어 [detectFrame] 에 넘길 길이. */
    const val FRAME_HEADER_SIZE = 4

    /**
     * ID3 태그로 시작하면 태그 뒤의 첫 프레임을 봐야 MP3 와 AAC(ADTS)를 가를 수 있다. 유튜브 HLS
     * 오디오를 받으면 ID3 타임스탬프 태그 뒤에 ADTS 가 온다. 태그가 [header] 를 넘어서면 null 이다 —
     * 호출자가 [id3PayloadOffset] 에서 더 읽어 [detectFrame] 으로 가른다.
     */
    fun detect(header: ByteArray): AudioContainer? = when {
        header.startsWith(4, "ftyp") -> AudioContainer.MP4
        header.startsWith(0, "ID3") -> id3PayloadOffset(header)
            ?.takeIf { it + FRAME_HEADER_SIZE <= header.size }
            ?.let { detectFrame(header.copyOfRange(it.toInt(), it.toInt() + FRAME_HEADER_SIZE)) }
        header.startsWith(0, "fLaC") -> AudioContainer.FLAC
        header.startsWith(0, "RIFF") && header.startsWith(8, "WAVE") -> AudioContainer.WAVE
        header.startsWith(0, "FORM") && (header.startsWith(8, "AIFF") || header.startsWith(8, "AIFC")) ->
            AudioContainer.AIFF
        header.startsWith(0, "caff") -> AudioContainer.CAF
        header.startsWith(0, "OggS") ->
            if (header.startsWith(OGG_FIRST_PACKET_OFFSET, "OpusHead")) AudioContainer.OGG_OPUS else AudioContainer.OGG
        header.startsWith(0, EBML_MAGIC) -> AudioContainer.WEBM
        header.startsWith(0, ASF_MAGIC) -> AudioContainer.ASF
        header.startsWith(0, "#!AMR") -> AudioContainer.AMR
        else -> detectFrame(header)
    }

    /** ID3v2 태그 뒤 첫 바이트의 위치. ID3 로 시작하지 않으면 null. */
    fun id3PayloadOffset(header: ByteArray): Long? {
        if (!header.startsWith(0, "ID3") || header.size < ID3_HEADER_SIZE) return null
        // 크기는 7비트씩 끊은 syncsafe 정수다. 플래그 0x10 이면 같은 크기의 꼬리말이 붙는다.
        val size = (6..9).fold(0L) { acc, i -> (acc shl 7) or (header[i].toLong() and 0x7F) }
        val footer = if (header[5].toInt() and 0x10 != 0) ID3_HEADER_SIZE else 0
        return ID3_HEADER_SIZE + size + footer
    }

    /** MPEG 오디오 프레임 머리. 11비트 동기 뒤의 layer 비트가 00 이면 ADTS(AAC), 아니면 MP3 다. */
    fun detectFrame(bytes: ByteArray): AudioContainer? {
        if (bytes.size < 2 || bytes[0] != 0xFF.toByte()) return null
        val second = bytes[1].toInt() and 0xFF
        return when {
            second and 0xE0 != 0xE0 -> null
            second and 0xF6 == 0xF0 -> AudioContainer.ADTS
            second and 0x06 != 0 -> AudioContainer.MP3
            else -> null
        }
    }

    /** 내용과 다른 확장자면 바로잡은 파일명, 맞으면 null. */
    fun correctedFileName(fileName: String, container: AudioContainer): String? {
        val base = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        if (extension.equals(container.extension, ignoreCase = true)) return null
        return "$base.${container.extension}"
    }

    private fun ByteArray.startsWith(offset: Int, text: String): Boolean =
        startsWith(offset, text.encodeToByteArray())

    private fun ByteArray.startsWith(offset: Int, bytes: ByteArray): Boolean =
        size >= offset + bytes.size && bytes.indices.all { this[offset + it] == bytes[it] }

    private const val ID3_HEADER_SIZE = 10
    private const val OGG_FIRST_PACKET_OFFSET = 28
    private val EBML_MAGIC = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())
    private val ASF_MAGIC = byteArrayOf(
        0x30, 0x26, 0xB2.toByte(), 0x75, 0x8E.toByte(), 0x66, 0xCF.toByte(), 0x11,
    )
}
