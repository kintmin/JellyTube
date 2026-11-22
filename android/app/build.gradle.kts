import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import com.kintmin.buildSrc.AppConfiguration
import com.kintmin.buildSrc.DeepLinkConstantsSource
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
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.chaquopy)
    alias(libs.plugins.sentry.android)
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

        // intent-filter 의 스킴·호스트는 DeepLinkConstants 에서 읽어 넣는다. 복제하면 한쪽만 바뀐다.
        manifestPlaceholders[ManifestPlaceholdersKey.DEEP_LINK_SCHEME] =
            DeepLinkConstantsSource.read(rootDir, "SCHEME")
        manifestPlaceholders[ManifestPlaceholdersKey.DEEP_LINK_HOST] =
            DeepLinkConstantsSource.read(rootDir, "HOST")
    }

    signingConfigs {
        // 어느 PC 에서 빌드해도 개발용 앱을 지우지 않고 덮어 설치할 수 있게 debug 키를 저장소에 둔다.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // 릴리스 키는 저장소 밖에 있다. 없는 클론에서도 나머지 빌드는 되고 release 는 서명 없이 나온다.
        val releaseStoreFile = findProperty(PropertyName.STORE_FILE)?.toString()
        if (releaseStoreFile != null) {
            create(SigningConfigName.RELEASE) {
                storeFile = file(releaseStoreFile)
                storePassword = findProperty(PropertyName.STORE_PASSWORD).toString()
                keyAlias = findProperty(PropertyName.KEY_ALIAS).toString()
                keyPassword = findProperty(PropertyName.KEY_PASSWORD).toString()
            }
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
            signingConfig = signingConfigs.findByName(SigningConfigName.RELEASE)
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

sentry {
    org.set("kintmin-personal")
    projectName.set("jellytube")
    // 토큰이 없는 클론에서도 release 빌드가 되도록, 그때는 mapping 을 올리지 않는다.
    val sentryAuthToken = findProperty(PropertyName.SENTRY_AUTH_TOKEN)?.toString()
    authToken.set(sentryAuthToken)
    autoUploadProguardMapping.set(sentryAuthToken != null)
    // SDK 는 shared:data 가 KMP SDK 로 가져온다. 플러그인이 또 넣으면 버전이 갈린다.
    autoInstallation { enabled.set(false) }
    tracingInstrumentation { enabled.set(false) }
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
    implementation(project(":shared:presentation"))
    implementation(project(":shared:log"))
    implementation(project(":shared:domain"))
    implementation(project(":shared:data"))
    implementation(project(":shared:file-share"))

    implementation(libs.koin.android)
    implementation(libs.koin.workmanager)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    // androidx compose BOM 을 같이 두면 androidx.compose.ui 버전이 CMP 포크와 충돌한다.
    // compose·navigation 은 선언하지 않는다.

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
