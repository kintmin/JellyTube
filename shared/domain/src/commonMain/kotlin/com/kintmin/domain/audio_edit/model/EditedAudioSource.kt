package com.kintmin.domain.audio_edit.model

/**
 * 편집본을 새 음원으로 저장할 때 쓰는 출처 문자열 규약.
 *
 * ```
 * editedAudio://{고유값}?from={원본 출처 원문}
 * ```
 *
 * - AUDIO_MEDIA.source는 중복 판정에 쓰이고 유니크 제약이 없다. 그래서 호출부가 고유값을 넣어준다.
 * - 원본 출처를 그대로 뒤에 붙이기 때문에, 편집본을 만든 뒤에도 **원본을 다시 다운로드할 수 있다**
 *   (source 문자열이 달라 이미 저장된 음원으로 판정되지 않는다).
 * - 원본 row id가 아니라 출처 문자열을 담는다. 원본이 삭제되면 id 참조는 가리킬 곳이 없어진다.
 *   출처는 FK가 아니라 표시용 정보라서 원본이 사라져도 아무것도 깨지지 않는다.
 * - URL 인코딩은 하지 않는다. 원본 출처가 항상 문자열 끝이라 마지막 구분자만 찾으면 되고,
 *   이 모듈에는 인코딩 수단이 없다.
 */
object EditedAudioSource {

    private const val SCHEME_PREFIX = "editedAudio://"
    private const val FROM_DELIMITER = "?from="

    fun build(uniqueId: String, originSource: String): String =
        "$SCHEME_PREFIX$uniqueId$FROM_DELIMITER$originSource"

    fun isEditedAudio(source: String): Boolean = source.startsWith(SCHEME_PREFIX)

    /**
     * 출처를 눌렀을 때 열 수 있는 웹 주소. 열 수 없으면 null.
     *
     * 편집본의 편집본처럼 출처가 중첩돼도(`editedAudio://A?from=editedAudio://B?from=https://...`)
     * 마지막 구분자 뒤를 보기 때문에 최종 원본 주소를 찾아낸다.
     * 기기에서 가져온 음원(`fileShare://`, `quickShare://`)은 열 곳이 없어 null이다.
     */
    fun webViewUrlOrNull(source: String): String? {
        val originSource = if (isEditedAudio(source)) {
            source.substringAfterLast(FROM_DELIMITER, missingDelimiterValue = "")
        } else {
            source
        }
        return originSource.takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        }
    }
}
