plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.chaquopy)
}

android {
    namespace = "com.kintmin.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    flavorDimensions += "abi"
    productFlavors {
        create("development") {
            dimension = "abi"
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        }
        create("production") {
            dimension = "abi"
            ndk { abiFilters += listOf("arm64-v8a") }
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
        pip {
            install("yt_dlp")
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}