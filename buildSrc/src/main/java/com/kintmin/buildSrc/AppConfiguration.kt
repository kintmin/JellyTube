package com.kintmin.buildSrc

object AppConfiguration {
    const val APPLICATION_ID = "com.kintmin.jellytube"
    const val RELEASE_APP_NAME = "JellyTube"
    const val DEBUG_APP_NAME = "JellyTube(개발)"
    const val COMPILE_SDK = 35
    const val TARGET_SDK = 35
    const val MIN_SDK = 26
    const val VERSION_CODE = 9
    const val VERSION_NAME = "1.0.9"
    /**
     * Python 3.12 이상은 armeabi-v7a (구버전 기기) 대응이 안 됨
     * Chaquopy version 15.0.1 news 참고: https://chaquo.com/chaquopy/news/
     */
    const val PYTHON_VERSION = "3.11"
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