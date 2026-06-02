import com.kintmin.buildSrc.AppConfiguration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // no external dependencies; this is a pure API module
        }
    }
}

android {
    namespace = "com.kintmin.log"
    compileSdk = AppConfiguration.COMPILE_SDK

    defaultConfig {
        minSdk = AppConfiguration.MIN_SDK
    }
}
