import com.kintmin.buildSrc.AppConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // AGP 9 부터 KMP 모듈에 com.android.library + androidTarget() 조합을 쓸 수 없다.
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // kotlin-parcelize 는 쓰지 않는다. @Parcelize 사용처가 0 건이다.
}

kotlin {
    android {
        namespace = "com.kintmin.presentation"
        compileSdk = AppConfiguration.COMPILE_SDK
        minSdk = AppConfiguration.MIN_SDK

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        // AGP KMP 플러그인에서 호스트(단위) 테스트는 opt-in 이다.
        withHostTestBuilder { }.configure { }

        // 이 플러그인은 buildConfig / res / buildTypes 를 지원하지 않는다.
        // BuildConfig.DEBUG 는 제거했고(디버그 탭 가시성은 app 이 주입), res 는 원래 없다.
        // proguard 파일은 소비자에게 전파되지 않는 사문이었으므로 옮기지 않았다.
    }

    iosArm64()
    iosSimulatorArm64()
    // iosX64 는 CMP 가 1.11.0-alpha01 이후 배포하지 않는다. shared:* 3모듈과 동일하게 제외.
    // Windows 에서는 iOS 태스크가 돌지 않는다. 선언만 유지해 소스셋 구조를 준비한다.

    sourceSets {
        commonMain.dependencies {
            // app 과 (3단계) iOS 가 Composable 을 작성하므로 CMP 아티팩트는 api 로 노출한다.
            api(project(":shared:domain"))
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.material3)
            api(libs.compose.material.icons.extended)
            // app 의 MainNavHost 가 NavHost / rememberNavController 를 쓴다.
            api(libs.jetbrains.navigation.compose)

            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.savedstate)
            // CMP 1.12 에서 ui-backhandler 가 deprecated 되어 이쪽으로 간다.
            implementation(libs.jetbrains.navigationevent.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            implementation(libs.io.coil.compose)   // coil3 는 멀티플랫폼이다
            implementation(libs.okio)              // Coil 입력 경로를 okio.Path 로 준다

            // PC 업로드 수신 경로. 서버 엔진은 플랫폼이 고른다
            implementation(project(":shared:file-share"))
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        androidMain.dependencies {
            // 임시 수용소. commonMain 승격이 끝나면 대부분 사라진다.
            implementation(libs.androidx.activity.compose)       // ActivityResult / BackHandler
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)  // lifecycleScope
            implementation(libs.androidx.palette.ktx)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }

        iosMain.dependencies {
            // Android 는 같은 서버를 android:app 의 포그라운드 서비스가 띄운다.
            implementation(libs.ktor.server.cio)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test)
        }
        iosTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        // commonTest 소스셋은 만들지 않는다. mockk 가 JVM 전용이라 iOS 컴파일이 깨진다.
    }
}

// Compose 프리뷰 렌더러(ComposeViewAdapter). 어노테이션(ui-tooling-preview)과 별개다.
// com.android.kotlin.multiplatform.library 는 variant 가 없어 debugImplementation 을 쓸 수 없다.
dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
