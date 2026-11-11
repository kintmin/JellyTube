package com.kintmin.domain.audio_media.format

/** 파일 앞부분만 보고 컨테이너를 가른다. 디코딩하지 않는다. 모르면 null. */
object AudioContainerSniffer {

    /** 판별에 필요한 앞부분 길이. Ogg Opus 의 `OpusHead` 가 28바이트째에 있다. */
    const val HEADER_SIZE = 64

    fun detect(header: ByteArray): AudioContainer? = when {
        header.startsWith(4, "ftyp") -> AudioContainer.MP4
        header.startsWith(0, "ID3") -> AudioContainer.MP3
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
        header.size >= 2 && header[0] == 0xFF.toByte() -> frameSyncContainer(header[1].toInt() and 0xFF)
        else -> null
    }

    /** 내용과 다른 확장자면 바로잡은 파일명, 맞거나 판별할 수 없으면 null. */
    fun correctedFileName(fileName: String, header: ByteArray): String? {
        val container = detect(header) ?: return null
        val base = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        if (extension.equals(container.extension, ignoreCase = true)) return null
        return "$base.${container.extension}"
    }

    // 11비트 동기 뒤의 layer 비트가 00 이면 ADTS(AAC), 아니면 MPEG 오디오 프레임이다.
    private fun frameSyncContainer(second: Int): AudioContainer? = when {
        second and 0xE0 != 0xE0 -> null
        second and 0xF6 == 0xF0 -> AudioContainer.ADTS
        second and 0x06 != 0 -> AudioContainer.MP3
        else -> null
    }

    private fun ByteArray.startsWith(offset: Int, text: String): Boolean =
        startsWith(offset, text.encodeToByteArray())

    private fun ByteArray.startsWith(offset: Int, bytes: ByteArray): Boolean =
        size >= offset + bytes.size && bytes.indices.all { this[offset + it] == bytes[it] }

    private const val OGG_FIRST_PACKET_OFFSET = 28
    private val EBML_MAGIC = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())
    private val ASF_MAGIC = byteArrayOf(
        0x30, 0x26, 0xB2.toByte(), 0x75, 0x8E.toByte(), 0x66, 0xCF.toByte(), 0x11,
    )
}
