import com.kintmin.buildSrc.AppConfiguration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.kintmin.log"
        compileSdk = AppConfiguration.COMPILE_SDK
        minSdk = AppConfiguration.MIN_SDK
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // no external dependencies; this is a pure API module
        }
    }
}
