package com.kintmin.presentation.deeplink

/**
 * 딥링크 주소에서 조각을 꺼낸다. 플랫폼 파서(`android.net.Uri` / `NSURL`)를 쓰지 않는 것은
 * 같은 주소를 두 플랫폼이 다르게 읽을 수 있어서다.
 */
internal object UrlParts {

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

    fun host(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null

        val authority = afterScheme.takeWhile { !it.isAuthorityTerminator() }
        val hostWithPort = authority.substringAfterLast('@')

        return hostWithPort.substringBefore(':').ifEmpty { null }
    }

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

    /** 한 글자가 여러 바이트일 수 있어 연속된 `%XX` 를 모아 UTF-8 로 해석한다. */
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

    /** unreserved(`A-Za-z0-9-_.~`)만 남기고 나머지는 UTF-8 바이트의 `%XX` 로 바꾼다. */
    fun encodeQueryValue(value: String): String {
        val encoded = StringBuilder(value.length)
        for (byte in value.encodeToByteArray()) {
            val char = byte.toInt().toChar()
            if (char.isAsciiLetter() || char.isAsciiDigit() || char in "-_.~") {
                encoded.append(char)
            } else {
                encoded.append('%')
                encoded.append(HEX_DIGITS[(byte.toInt() shr 4) and 0x0F])
                encoded.append(HEX_DIGITS[byte.toInt() and 0x0F])
            }
        }
        return encoded.toString()
    }

        private fun Char.isAuthorityTerminator(): Boolean = this == '/' || this == '\\' || this == '?' || this == '#'

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

    private const val HEX_DIGITS = "0123456789ABCDEF"

    private fun Char.hexValue(): Int? = when (this) {
        in '0'..'9' -> this - '0'
        in 'a'..'f' -> this - 'a' + 10
        in 'A'..'F' -> this - 'A' + 10
        else -> null
    }
}
