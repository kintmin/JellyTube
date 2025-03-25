plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kintmin.dataapi"
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

dependencies {
    implementation(project(":domain"))

    implementation(project(":data:localDatabase"))
    implementation(project(":data:localFile"))
    implementation(project(":data:network"))
    implementation(project(":data:pythonBridge"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.core)
    ksp(libs.hilt.core.compiler)
}