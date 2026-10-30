import com.kintmin.buildSrc.AppConfiguration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.atomicfu)
}

kotlin {
    android {
        namespace = "com.kintmin.domain"
        compileSdk = AppConfiguration.COMPILE_SDK
        minSdk = AppConfiguration.MIN_SDK

        // AGP KMP 플러그인에서 호스트(단위) 테스트는 opt-in 이다.
        withHostTestBuilder { }.configure { }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:log"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
