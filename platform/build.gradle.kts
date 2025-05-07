plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.kintmin.platform"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.hilt.android)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
}