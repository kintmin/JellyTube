package com.kintmin.log

const val HIDDEN_TEXT = "<비밀>"

/** 비밀 데이터면 가린 글자를 낸다. 판정은 값을 찍는 쪽이 데이터로 하고, 판정할 수 없으면 비밀로 둔다. */
fun secret(value: Any?, isSecret: Boolean): String = if (isSecret) HIDDEN_TEXT else value.toString()
