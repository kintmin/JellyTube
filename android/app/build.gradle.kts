import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.kintmin.buildSrc.AppConfiguration
import com.kintmin.buildSrc.ManifestPlaceholdersKey
import com.kintmin.buildSrc.PropertyName
import com.kintmin.buildSrc.SigningConfigName
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.chaquopy)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.kintmin.jellytube"
    compileSdk = AppConfiguration.COMPILE_SDK

    defaultConfig {
        applicationId = AppConfiguration.APPLICATION_ID
        minSdk = AppConfiguration.MIN_SDK
        targetSdk = AppConfiguration.TARGET_SDK
        versionCode = AppConfiguration.VERSION_CODE
        versionName = AppConfiguration.VERSION_NAME
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create(SigningConfigName.RELEASE) {
            storeFile = file(property(PropertyName.STORE_FILE).toString())
            storePassword = property(PropertyName.STORE_PASSWORD).toString()
            keyAlias = property(PropertyName.KEY_ALIAS).toString()
            keyPassword = property(PropertyName.KEY_PASSWORD).toString()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders[ManifestPlaceholdersKey.APP_LABEL] = AppConfiguration.RELEASE_APP_NAME
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(SigningConfigName.RELEASE)
            // Crashlytics 확장은 android{} 가 아니라 buildType 에 등록된다.
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
        }
        debug {
            ndk { abiFilters += listOf("x86_64") }
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders[ManifestPlaceholdersKey.APP_LABEL] = AppConfiguration.DEBUG_APP_NAME
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

/**
 * 산출 APK 파일명을 "JellyTube_<versionName>(<versionCode>)_<variant>_<시각>.apk" 로 바꾼다.
 *
 * AGP 9 부터 variant.outputs 의 outputFileName 을 직접 고치던 internal API(VariantOutputImpl)가
 * 사라져서, 공개 API 인 Artifacts transform 으로 APK 를 복사하며 이름을 붙인다.
 */
abstract class RenameApkTask : DefaultTask() {

    @get:InputFiles
    abstract val apkFolder: DirectoryProperty

    @get:OutputDirectory
    abstract val outFolder: DirectoryProperty

    @get:Input
    abstract val variantName: Property<String>

    @get:Internal
    abstract val transformationRequest: Property<ArtifactTransformationRequest<RenameApkTask>>

    @TaskAction
    fun taskAction() {
        val time = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime()
        val timeString = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH.mm.ss").format(time)
        transformationRequest.get().submit(this) { builtArtifact ->
            val target = File(
                outFolder.get().asFile,
                "JellyTube_${AppConfiguration.VERSION_NAME}(${AppConfiguration.VERSION_CODE})_${variantName.get()}_$timeString.apk"
            )
            File(builtArtifact.outputFile).copyTo(target, overwrite = true)
            target
        }
    }
}

androidComponents {
    onVariants { variant ->
        val renameApk = tasks.register<RenameApkTask>(
            "rename${variant.name.replaceFirstChar(Char::uppercaseChar)}Apk"
        ) {
            variantName.set(variant.name)
        }
        val request = variant.artifacts.use(renameApk)
            .wiredWithDirectories(RenameApkTask::apkFolder, RenameApkTask::outFolder)
            .toTransformMany(SingleArtifact.APK)
        renameApk.configure { transformationRequest.set(request) }
    }
}

chaquopy {
    sourceSets {
        getByName("main") {
            srcDir("../../shared/data/src/python")
        }
    }
    defaultConfig {
        version = AppConfiguration.PYTHON_VERSION
        pip {
            install("yt-dlp")
            install("pykakasi")
        }
        // pykakasi 는 한자 사전(kanwadict4.db)을 실제 파일 경로로 연다.
        // Chaquopy 는 기본적으로 패키지 데이터를 asset zip 에서 바로 읽어 importlib.resources 가
        // AssetPath 를 돌려주는데, pykakasi 가 os.fspath 로 실경로를 요구해 실패한다.
        // 해당 패키지를 파일시스템으로 추출해 실제 경로가 잡히도록 한다.
        extractPackages("pykakasi")
    }
}

dependencies {
    implementation(project(":android:presentation"))
    implementation(project(":android:platform"))
    implementation(project(":shared:log"))
    implementation(project(":shared:domain"))
    implementation(project(":shared:data"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.koin.android)
    implementation(libs.koin.workmanager)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.splashscreen)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
