package com.kintmin.domain.youtube

/**
 * 다운로드 대상이 되는 유튜브 영상 URL 인지 판정한다.
 *
 * android.net.Uri 없이 순수 문자열로 파싱한다. 플랫폼 URI 파서는 관용적이라
 * 호스트/경로 판정 결과가 플랫폼마다 달라질 수 있는데, 이 판정은 비즈니스
 * 규칙이므로 어디서나 같은 답이 나와야 한다.
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

    /** scheme 과 authority 를 걷어내고 호스트만 돌려준다. 포트·userinfo 는 제거한다. */
    private fun hostOf(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null

        val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val hostWithPort = authority.substringAfterLast('@')
        val host = hostWithPort.substringBefore(':')

        return host.ifEmpty { null }
    }

    /** 경로의 첫 세그먼트를 돌려준다. 경로가 없으면 null. */
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
