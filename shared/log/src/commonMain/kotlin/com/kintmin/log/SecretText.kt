package com.kintmin.log

/** 비밀모드에서 가릴 값을 표시한다. 예외 메시지에는 쓰지 않는다 — 수집기가 예외를 걷어 내지 않고 그대로 올린다. */
fun secret(value: Any?): String = "$SECRET_START$value$SECRET_END"

// 사용자 입력에 나올 일이 없는 사용 영역 문자를 경계로 쓴다.
val SECRET_START = Char(0xE000)
val SECRET_END = Char(0xE001)
