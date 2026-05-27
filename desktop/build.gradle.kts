import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.uiToolingPreview)
    implementation(compose.components.resources)

    implementation(project(":shared:file-share"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Ktor client (HTTP 업로드)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // mDNS NSD discovery
    implementation(libs.jmdns)
}

compose.resources {
    packageOfResClass = "com.kintmin.desktop.resources"
}

compose.desktop {
    application {
        mainClass = "com.kintmin.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "JellyTube File Share"
            packageVersion = "1.0.0"
            vendor = "kintmin"
            description = "Desktop file sharing client for JellyTube"

            windows {
                menu = true
                shortcut = true
                perUserInstall = true
                dirChooser = true
                upgradeUuid = "8ebf0665-d668-4ea7-92f1-cc8da8a3b9da"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}
