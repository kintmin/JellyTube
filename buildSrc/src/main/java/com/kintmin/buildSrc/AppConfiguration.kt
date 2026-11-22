package com.kintmin.buildSrc

object AppConfiguration {
    const val APPLICATION_ID = "com.kintmin.jellytube"
    const val RELEASE_APP_NAME = "JellyTube"
    const val DEBUG_APP_NAME = "JellyTube(개발)"
    // CMP 1.12.2 가 끌어오는 androidx compose 1.12.1 이 compileSdk 37 이상을 요구한다.
    // compileSdk 는 컴파일 전용이라 런타임 동작(targetSdk)과 무관하다.
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 36
    const val MIN_SDK = 26
    const val VERSION_CODE = 19
    const val VERSION_NAME = "1.2.2"
    // MSI 는 숫자 세 마디만 받고 앞 두 마디는 255 이하다. 올리지 않으면 업그레이드 설치가 안 된다.
    const val DESKTOP_VERSION_NAME = "1.0.1"
    /**
     * Python 3.12 이상은 armeabi-v7a (구버전 기기) 대응이 안 됨
     * Chaquopy version 15.0.1 news 참고: https://chaquo.com/chaquopy/news/
     */
    const val PYTHON_VERSION = "3.11"
}

// xml 사용되는 것 확인
object ManifestPlaceholdersKey {
    const val APP_LABEL = "appLabel"
    const val DEEP_LINK_SCHEME = "deepLinkScheme"
    const val DEEP_LINK_HOST = "deepLinkHost"
}

// gradle에 있는 gradle.properties 나 루트에 있는 local.properties 참고
object PropertyName {
    const val STORE_FILE = "JELLY_TUBE_KEYSTORE_FILE"
    const val STORE_PASSWORD = "JELLY_TUBE_KEYSTORE_PASSWORD"
    const val KEY_ALIAS = "JELLY_TUBE_KEY_ALIAS"
    const val KEY_PASSWORD = "JELLY_TUBE_KEY_PASSWORD"
    const val SENTRY_AUTH_TOKEN = "JELLY_TUBE_SENTRY_AUTH_TOKEN"
}

object SigningConfigName {
    const val RELEASE = "release"
}