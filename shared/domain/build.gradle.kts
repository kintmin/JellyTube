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
            api(project(":log:log-api"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.kintmin.domain"
    compileSdk = AppConfiguration.COMPILE_SDK

    defaultConfig {
        minSdk = AppConfiguration.MIN_SDK
    }
}
