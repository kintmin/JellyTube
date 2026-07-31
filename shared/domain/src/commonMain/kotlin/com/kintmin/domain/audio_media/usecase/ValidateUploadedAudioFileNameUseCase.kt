package com.kintmin.domain.audio_media.usecase

/**
 * 업로드 요청이 헤더로 준 파일 이름을 검증하고 저장에 쓸 형태로 정규화한다.
 *
 * yt-dlp가 내려주는 이름과 달리 이 값은 클라이언트가 임의로 채우므로 신뢰할 수 없다.
 * 그렇다고 확장자를 형식 화이트리스트로 걸러낼 수도 없다 — 확장자를 변형하면 확장자를 신뢰하는
 * iOS(AVFoundation) 재생이 깨지기 때문에 "그대로 쓴다"가 현재 구조다.
 *
 * 그래서 화이트리스트 대신, 값이 **쓰일 위치**에 따라 다르게 다룬다.
 *
 * - **확장자**: 실제 저장 파일명(`<UUID>.<ext>`)의 일부라 파일시스템에 닿는다. 정규화로 문자를
 *   지우면 틀린 확장자가 만들어져 재생이 조용히 깨지므로, 부적합하면 고치지 않고 **거부**한다.
 * - **이름 부분**: 파일시스템에 닿지 않고 제목 폴백으로만 쓰인다. 제어 문자만 제거하고 길이를
 *   제한하는 **정규화**로 끝낸다. 제목에는 `:`나 `?`가 정상적으로 들어갈 수 있어
 *   파일시스템 기준을 적용하면 멀쩡한 제목이 망가진다.
 *
 * 폴백(`upload.mp3`, `mp3`)은 두지 않는다. 확장자를 추측해 저장하면 실패가 재생 시점까지 미뤄지므로
 * 호출 측이 사용자에게 즉시 알리도록 실패를 그대로 돌려준다.
 */
class ValidateUploadedAudioFileNameUseCase {

    operator fun invoke(rawFileName: String?): Result<String> {
        // 경로 성분과 제어 문자는 정규화로 걷어낸다. 여기서 지워도 확장자 의미가 바뀌지 않는다.
        val fileName = (rawFileName ?: "")
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .removeControlChars()
            .trim()

        if (fileName.isEmpty() || fileName.all { it == '.' }) {
            return Result.failure(InvalidUploadedFileName())
        }

        val ext = fileName.substringAfterLast('.', "")
        if (ext.isEmpty()) {
            return Result.failure(MissingUploadedFileExtension())
        }
        if (!VALID_EXTENSION.matches(ext)) {
            // 되돌려주는 값도 이미 검증에 실패한 문자열이므로 길이를 제한해 그대로 흘리지 않는다.
            return Result.failure(InvalidUploadedFileExtension(ext.clampLength(MAX_EXTENSION_LENGTH)))
        }

        val name = fileName.substringBeforeLast('.').trim()
        if (name.isEmpty() || name.all { it == '.' }) {
            return Result.failure(InvalidUploadedFileName())
        }

        return Result.success("${name.clampLength(MAX_NAME_LENGTH)}.$ext")
    }

    /**
     * AOSP `FileUtils`가 파일명에서 거부하는 제어 문자(`0x00`-`0x1F`, `0x7F`)를 제거한다.
     * https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/os/FileUtils.java
     */
    private fun String.removeControlChars(): String = filter { it.code > 0x1F && it.code != 0x7F }

    /** 서로게이트 페어를 쪼개지 않고 [maxLength]자로 자른다. 쪼개면 인코딩 시 U+FFFD가 생긴다. */
    private fun String.clampLength(maxLength: Int): String {
        if (length <= maxLength) return this
        val end = if (this[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
        return substring(0, end)
    }

    private companion object {

        /** 실제 오디오 컨테이너 확장자는 길어도 5자(`flac`, `webm`)다. 표준값이 아니라 넉넉히 잡은 상한이다. */
        const val MAX_EXTENSION_LENGTH = 16

        /** 제목 폴백으로만 쓰이므로 파일시스템 상한이 아니라 표시 문자열로서의 안전 상한이다. */
        const val MAX_NAME_LENGTH = 255

        /**
         * 확장자에 허용할 문자 집합.
         *
         * POSIX **Portable Filename Character Set**에서 점(`.`)만 뺀 집합이다. 확장자 토큰은
         * 정의상 점을 포함할 수 없으므로 실질적으로 표준 집합 전체다.
         * > A B C ... Z a b c ... z 0 1 ... 9 . _ -
         *
         * IEEE Std 1003.1-2017 (Open Group Base Specifications Issue 7, 2018 edition) §3.282
         * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap03.html#tag_03_282
         *
         * 이 집합은 AOSP `FileUtils.isValidFatFilenameChar`가 거부하는 문자
         * (`0x00`-`0x1F`, `0x7F`, `" * / : < > ? \ |`)를 모두 배제하므로,
         * 착탈식 SD처럼 exFAT/FAT32로 마운트된 외부 저장소에서도 안전하다.
         * https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/os/FileUtils.java
         *
         * 주의 1: 앵커(`^`, `$`)를 쓰지 않는다. Kotlin `Regex.matches`는 입력 전체 일치를 요구한다.
         * 주의 2: `Char.isLetterOrDigit()`은 유니코드 기준이라 `한`도 통과시킨다. 명시적 ASCII 범위여야 한다.
         */
        val VALID_EXTENSION = Regex("[A-Za-z0-9_-]{1,$MAX_EXTENSION_LENGTH}")
    }
}

class InvalidUploadedFileName(
    override val message: String = "파일 이름을 알 수 없어 저장할 수 없습니다.",
) : Exception()

class MissingUploadedFileExtension(
    override val message: String = "확장자가 없는 파일은 저장할 수 없습니다.",
) : Exception()

class InvalidUploadedFileExtension(ext: String) : Exception() {
    override val message: String = "확장자에 쓸 수 없는 문자가 있습니다: .$ext"
}
