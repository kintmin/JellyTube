package com.kintmin.domain.youtube

/**
 * 다운로드 대상이 되는 유튜브 영상 URL 인지 판정한다. 플랫폼 URI 파서를 쓰지 않는 것은
 * 관용적이라 같은 주소를 플랫폼마다 다르게 읽을 수 있어서다.
 */
object YoutubeUrl {

    private val DOWNLOADABLE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "youtu.be",
    )
    private val DOWNLOADABLE_FIRST_PATH_SEGMENTS = setOf("watch", "shorts")

    fun isDownloadable(url: String): Boolean {
        val host = hostOf(url)?.lowercase() ?: return false
        val firstPathSegment = firstPathSegmentOf(url) ?: return false

        return host in DOWNLOADABLE_HOSTS && firstPathSegment in DOWNLOADABLE_FIRST_PATH_SEGMENTS
    }

    /** 포트·userinfo 는 떼어낸다. */
    private fun hostOf(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null

        val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val hostWithPort = authority.substringAfterLast('@')
        val host = hostWithPort.substringBefore(':')

        return host.ifEmpty { null }
    }

    private fun firstPathSegmentOf(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null

        val authorityEnd = afterScheme.indexOf('/')
        if (authorityEnd == -1) return null

        val path = afterScheme.substring(authorityEnd + 1)
            .substringBefore('?')
            .substringBefore('#')

        return path.substringBefore('/').ifEmpty { null }
    }
}
