package com.kintmin.presentation.platform

/** 새 버전의 설치 파일을 사용자에게 넘기는 방식. */
enum class AppUpdateInstaller {
    /** 브라우저로 받고 사용자가 열어 설치한다. */
    Browser,

    /** SideStore 가 받아 서명하고 설치한다. */
    SideStore,
    ;

    fun installUri(downloadUrl: String): String = when (this) {
        Browser -> downloadUrl
        SideStore -> "sidestore://install?url=${encodeQueryValue(downloadUrl)}"
    }
}

expect val appUpdateInstaller: AppUpdateInstaller

private fun encodeQueryValue(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val char = byte.toInt().toChar()
        if (byte >= 0 && (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._~")) {
            append(char)
        } else {
            append('%').append(byte.toUByte().toString(16).uppercase().padStart(2, '0'))
        }
    }
}
