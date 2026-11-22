package com.kintmin.data.log

import com.kintmin.log.SECRET_END
import com.kintmin.log.SECRET_START

internal const val HIDDEN_TEXT = "<비밀>"

private val SECRET_REGEX = Regex("$SECRET_START([^$SECRET_END]*)$SECRET_END")

internal fun revealSecrets(text: String, hide: Boolean): String =
    SECRET_REGEX.replace(text) { if (hide) HIDDEN_TEXT else it.groupValues[1] }
