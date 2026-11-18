package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.InvalidUploadedFileNameException

/**
 * PC 업로드 헤더의 파일 이름을 저장할 형태로 바꾼다. 확장자는 저장 파일명(`<UUID>.<ext>`)에 그대로 들어가므로
 * 고치지 않고 거부한다. 고치거나 추측하면 확장자를 믿는 iOS 재생이 조용히 깨진다.
 * 이름 부분은 제목으로만 쓰여 파일시스템에 닿지 않는다. 제어 문자만 지우고 길이를 자른다.
 */
class ValidateUploadedAudioFileNameUseCase {

    operator fun invoke(rawFileName: String?): Result<String> {
        val fileName = rawFileName.orEmpty()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .filter { it.code > 0x1F && it.code != 0x7F }
            .trim()
        val name = fileName.substringBeforeLast('.').trim()
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        return when {
            fileName.all { it == '.' } -> invalid(UNKNOWN_NAME_MESSAGE)
            extension.isEmpty() -> invalid("확장자가 없는 파일은 저장할 수 없습니다.")
            !VALID_EXTENSION.matches(extension) ->
                invalid("확장자에 쓸 수 없는 문자가 있습니다: .${extension.take(MAX_EXTENSION_LENGTH)}")
            name.all { it == '.' } -> invalid(UNKNOWN_NAME_MESSAGE)
            else -> Result.success("${name.clampLength(MAX_NAME_LENGTH)}.$extension")
        }
    }

    private fun invalid(message: String) = Result.failure<String>(InvalidUploadedFileNameException(message))

    // 서로게이트 쌍을 쪼개면 인코딩할 때 U+FFFD 가 생긴다.
    private fun String.clampLength(maxLength: Int): String {
        if (length <= maxLength) return this
        val end = if (this[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
        return substring(0, end)
    }

    private companion object {
        const val MAX_EXTENSION_LENGTH = 16
        const val MAX_NAME_LENGTH = 255
        const val UNKNOWN_NAME_MESSAGE = "파일 이름을 알 수 없어 저장할 수 없습니다."

        // POSIX Portable Filename Character Set 에서 점을 뺀 ASCII 집합. FAT 금지 문자를 모두 배제한다.
        val VALID_EXTENSION = Regex("[A-Za-z0-9_-]{1,$MAX_EXTENSION_LENGTH}")
    }
}
