package com.kintmin.log.model

/** 수집기에 Issue 로 올라간다. 예외가 있으면 넘겨야 스택이 남는다. */
class ErrorLog(val tag: String, val message: String, val throwable: Throwable? = null)
