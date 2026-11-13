import com.kintmin.buildSrc.AppConfiguration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "com.kintmin.data"
        compileSdk = AppConfiguration.COMPILE_SDK
        minSdk = AppConfiguration.MIN_SDK

        // AGP KMP 플러그인에서 테스트는 opt-in 이다.
        withHostTestBuilder { }.configure { }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-rules.pro")
            }
        }

        packaging {
            resources {
                excludes += setOf(
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*"
                )
            }
        }
    }
    // 프레임워크와 SKIE 는 iOS Composition Root(`:ios:shared`)가 소유한다.
    // iOS 프레임워크는 하나여야 하고, 그 하나는 data 와 presentation 을 함께 보는
    // 모듈이어야 한다. 이 모듈은 klib 만 내놓는다.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:file-share"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // PC 업로드 수신 경로. Android 는 엔진을 android:app 의 포그라운드 서비스가 띄운다.
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.koin.core)
            implementation(libs.okio)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.android)
            implementation(libs.mlkit.translate)
            implementation(libs.androidx.media3.transformer)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.ktor.server.cio)
        }

        iosTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation("com.github.axet:TarsosDSP:2.4-1@jar")
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.junit)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
