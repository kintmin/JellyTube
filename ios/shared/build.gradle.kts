import com.kintmin.buildSrc.DeepLinkConstantsSource

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.skie)
}

/**
 * iOS 프레임워크는 **하나여야 한다** — 둘로 쪼개면 Kotlin/Native 런타임과 Koin 이 분리된다.
 * 그 프레임워크는 `data` 와 `presentation` 을 함께 봐야 하는데 두 모듈은 서로를 모른다.
 */
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            // Swift 의 `import shared` 를 유지한다.
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
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Info.plist 는 Xcode 소유라 Manifest 처럼 주입할 수 없다. 어긋나면 iOS 딥링크가 조용히 죽는다.
val expectedAppScheme = DeepLinkConstantsSource.read(rootDir, "APP_SCHEME")
val infoPlistFile = rootProject.file("ios/app/JellyTubeIos/Info.plist")

val verifyDeepLinkScheme = tasks.register("verifyDeepLinkScheme") {
    inputs.file(infoPlistFile)
    inputs.property("appScheme", expectedAppScheme)
    doLast {
        val registered = infoPlistFile.readText()
            .substringAfter("<key>CFBundleURLSchemes</key>", "")
            .substringAfter("<array>", "")
            .substringBefore("</array>", "")
        check("<string>$expectedAppScheme</string>" in registered) {
            "Info.plist 의 CFBundleURLSchemes 에 '$expectedAppScheme' 가 없다 — " +
                "DeepLinkConstants.APP_SCHEME 과 맞춘다."
        }
    }
}

tasks.named("check") { dependsOn(verifyDeepLinkScheme) }
