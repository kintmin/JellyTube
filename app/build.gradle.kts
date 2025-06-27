plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.kintmin.jellytube"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kintmin.jellytube"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".dev"
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    flavorDimensions += "abi"
    productFlavors {
        create("development") {
            dimension = "abi"
            ndk { abiFilters += listOf("arm64-v8a", "x86_64", "armeabi-v7a") }
        }
        create("production") {
            dimension = "abi"
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
    }
}

dependencies {
    implementation(project(":presentation"))
    implementation(project(":platform"))
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.hilt.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.splashscreen)
}