plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.skie)
}

/**
 * iOS Composition Root. `android:app` / `desktop` 과 같은 자리다.
 *
 * iOS 프레임워크는 **하나여야 한다** — 둘로 쪼개면 Kotlin/Native 런타임과 Koin 이 분리된다.
 * 그 프레임워크는 `data` 와 `presentation` 을 함께 봐야 하는데 두 모듈은 서로를 모른다
 * (레이어 규율). 둘을 동시에 의존하는 모듈이 이 모듈이고, 그것이 신설 이유의 전부다.
 *
 * 여기 두는 것: 프레임워크·SKIE 설정, Koin 조립, Compose 진입점, 그리고 `domain` 에
 * 플랫폼 구현을 넣지 않기 위한 도메인 포트의 iOS 구현.
 * 플랫폼 포트(`presentation/port`)의 iOS 구현은 **`presentation` 의 iosMain** 에 둔다 —
 * iOS 구현은 `Context` 류가 필요 없고 Kotlin/Native 가 UIKit·AVFoundation·CoreMotion
 * 인터롭을 기본 제공하므로 인터페이스와 같은 모듈에서 구현된다.
 */
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            // Swift 의 `import shared` 를 유지한다. 과거 :shared:data 가 쓰던 이름이다.
            baseName = "shared"
            // Swift 가 직접 구현·호출하는 타입이 있는 모듈만 export 한다.
            // export 하지 않으면 코드는 프레임워크에 들어가지만 헤더에 나오지 않는다.
            export(project(":shared:domain"))
            export(project(":shared:log"))
            export(project(":shared:data"))
        }
    }

    sourceSets {
        // 전 타깃이 iOS 라 commonMain 은 비워 두고 iosMain 에만 쓴다.
        // UIKit 등 platform.* 인터롭이 필요해 commonMain 에는 코드를 둘 수 없다.
        iosMain.dependencies {
            // export 대상은 api 로 선언해야 한다.
            api(project(":shared:domain"))
            api(project(":shared:log"))
            api(project(":shared:data"))
            api(project(":shared:presentation"))
            implementation(project(":shared:file-share"))

            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }

        iosTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
