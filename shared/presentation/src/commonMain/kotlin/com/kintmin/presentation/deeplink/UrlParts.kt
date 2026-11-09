package com.kintmin.presentation.deeplink

/**
 * 딥링크 주소에서 조각을 꺼낸다.
 *
 * `android.net.Uri` / `NSURL` 을 쓰지 않는다. 플랫폼 파서는 관용적이라 같은 주소를 두
 * 플랫폼이 다르게 읽을 수 있는데, **딥링크 판정은 어디서나 같은 답이 나와야 한다.**
 * `shared:domain` 의 `YoutubeUrl` 이 같은 이유로 이미 직접 파싱한다.
 *
 * **정규화하지 않는다** — IDNA 변환·호스트 퍼센트 디코딩·상대경로 해석을 하지 않는다.
 * 모르는 형태는 "다른 호스트" 로 떨어져야 하고, 정규화를 거쳐 통과해서는 안 된다.
 *
 * IPv6 리터럴(`[::1]:80`)은 지원하지 않는다. 호스트가 깨진 값으로 나와 판정에서 떨어지는데,
 * 딥링크 호스트는 고정 도메인 하나라 그 실패가 맞는 결과다.
 */
internal object UrlParts {

    /**
     * 스킴. RFC 3986 대로 `.` 도 허용하므로 `scheme.env` 형태의 커스텀 스킴이 통째로 나온다.
     * **소문자로 바꾸지 않는다** — 바꾸면 플랫폼 파서보다 관용적이 되어 판정이 느슨해진다.
     */
    fun scheme(url: String): String? {
        val colonIndex = url.indexOf(':')
        if (colonIndex <= 0) return null

        val candidate = url.substring(0, colonIndex)
        if (!candidate[0].isAsciiLetter()) return null
        if (!candidate.all { it.isAsciiLetter() || it.isAsciiDigit() || it == '+' || it == '-' || it == '.' }) {
            return null
        }
        return candidate
    }

    /**
     * 호스트. 포트와 userinfo 는 떼어낸다.
     *
     * **userinfo 는 마지막 `@` 까지다.** 첫 `@` 로 자르면 `https://jellytube.com@evil.com` 의
     * 호스트가 신뢰 도메인으로 읽혀 통과한다.
     */
    fun host(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null

        val authority = afterScheme.takeWhile { !it.isAuthorityTerminator() }
        val hostWithPort = authority.substringAfterLast('@')

        return hostWithPort.substringBefore(':').ifEmpty { null }
    }

    /** 경로 세그먼트. 플랫폼 파서와 같게 퍼센트 디코딩한 값을 돌려준다(`+` 는 그대로). */
    fun pathSegments(url: String): List<String> {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return emptyList()

        val authorityEnd = afterScheme.indexOfFirst { it == '/' || it == '\\' }
        if (authorityEnd == -1) return emptyList()

        val path = afterScheme.substring(authorityEnd)
            .substringBefore('?')
            .substringBefore('#')

        return path.split('/', '\\')
            .filter { it.isNotEmpty() }
            .map { percentDecode(it) }
    }

    /**
     * 쿼리 파라미터. 값은 플랫폼 파서와 같게 퍼센트 디코딩하고 `+` 를 공백으로 바꾼다.
     *
     * **같은 키가 여럿이면 첫 번째가 이긴다.** 뒤가 앞을 덮으면 검증을 통과할 값을 앞에 두고
     * 진짜 쓰고 싶은 값을 뒤에 붙이는 수법이 성립한다.
     */
    fun queryParameter(url: String, name: String): String? {
        val query = url.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
        if (query.isEmpty()) return null

        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            if (percentDecode(pair.substringBefore('='), plusAsSpace = true) != name) continue

            val rawValue = if (pair.contains('=')) pair.substringAfter('=') else ""
            return percentDecode(rawValue, plusAsSpace = true)
        }
        return null
    }

    /**
     * 퍼센트 디코딩. 한 글자가 여러 바이트일 수 있어 연속된 `%XX` 를 모아 UTF-8 로 해석한다.
     *
     * 깨진 `%` 는 그대로 남긴다 — 디코딩에 실패했다고 주소를 바꾸면 안 된다.
     */
    fun percentDecode(value: String, plusAsSpace: Boolean = false): String {
        val hasPlusToConvert = plusAsSpace && value.contains('+')
        if (!value.contains('%') && !hasPlusToConvert) return value

        val decoded = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            when {
                char == '%' -> {
                    val bytes = ArrayList<Byte>(4)
                    var cursor = index
                    while (cursor + 2 < value.length && value[cursor] == '%') {
                        val high = value[cursor + 1].hexValue() ?: break
                        val low = value[cursor + 2].hexValue() ?: break
                        bytes.add(((high shl 4) or low).toByte())
                        cursor += 3
                    }
                    if (bytes.isEmpty()) {
                        decoded.append(char)
                        index++
                    } else {
                        decoded.append(bytes.toByteArray().decodeToString())
                        index = cursor
                    }
                }
                hasPlusToConvert && char == '+' -> {
                    decoded.append(' ')
                    index++
                }
                else -> {
                    decoded.append(char)
                    index++
                }
            }
        }
        return decoded.toString()
    }

    /** WHATWG URL 과 같게 역슬래시도 authority 의 끝으로 본다. 맞추지 않으면 앱이 보는 호스트와 실제로 열리는 호스트가 갈린다. */
    private fun Char.isAuthorityTerminator(): Boolean = this == '/' || this == '\\' || this == '?' || this == '#'

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

    private fun Char.hexValue(): Int? = when (this) {
        in '0'..'9' -> this - '0'
        in 'a'..'f' -> this - 'a' + 10
        in 'A'..'F' -> this - 'A' + 10
        else -> null
    }
}
