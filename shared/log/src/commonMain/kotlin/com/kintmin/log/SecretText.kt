package com.kintmin.log

/** 비밀모드에서 가릴 값을 표시한다. 예외 메시지에는 쓰지 않는다 — 수집기가 예외를 걷어 내지 않고 그대로 올린다. */
fun secret(value: Any?): String = "$SECRET_START$value$SECRET_END"

/** [secret] 표시를 [hide] 면 가린 글자로, 아니면 원문으로 바꾼다. */
fun revealSecrets(text: String, hide: Boolean): String =
    SECRET_REGEX.replace(text) { if (hide) HIDDEN_TEXT else it.groupValues[1] }

const val HIDDEN_TEXT = "<비밀>"

// 사용자 입력에 나올 일이 없는 사용 영역 문자를 경계로 쓴다.
private const val SECRET_START = ''
private const val SECRET_END = ''
private val SECRET_REGEX = Regex("$SECRET_START([^$SECRET_END]*)$SECRET_END")
