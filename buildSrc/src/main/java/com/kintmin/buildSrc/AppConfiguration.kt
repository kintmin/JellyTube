package com.kintmin.buildSrc

object AppConfiguration {
    const val RELEASE_APP_NAME = "JellyTube"
    const val DEBUG_APP_NAME = "JellyTube(개발)"
    const val COMPILE_SDK = 34
    const val TARGET_SDK = 34
    const val MIN_SDK = 26
    const val VERSION_CODE = 4
    const val VERSION_NAME = "1.0.4"
    const val PYTHON_VERSION = "3.8"    // 구버전 Android 기기 대응된 최대 파이썬 버전
}

// xml 사용되는 것 확인
object ManifestPlaceholdersKey {
    const val APP_LABEL = "appLabel"
}

// gradle에 있는 gradle.properties 나 루트에 있는 local.properties 참고
object PropertyName {
    const val STORE_FILE = "JELLY_TUBE_KEYSTORE_FILE"
    const val STORE_PASSWORD = "JELLY_TUBE_KEYSTORE_PASSWORD"
    const val KEY_ALIAS = "JELLY_TUBE_KEY_ALIAS"
    const val KEY_PASSWORD = "JELLY_TUBE_KEY_PASSWORD"
}

object SigningConfigName {
    const val RELEASE = "release"
}