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
    alias(libs.plugins.androidx.baselineprofile)
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
    // baselineprofile 플러그인이 release 를 본떠 만드는 빌드 타입이다.
    onVariants { variant ->
        if (variant.buildType in setOf("benchmarkRelease", "nonMinifiedRelease")) {
            variant.applicationId.set(AppConfiguration.APPLICATION_ID + AppConfiguration.PERF_APPLICATION_ID_SUFFIX)
            variant.manifestPlaceholders.put(ManifestPlaceholdersKey.APP_LABEL, AppConfiguration.PERF_APP_NAME)
        }
    }

    onVariants(selector().withBuildType("release")) { variant ->
        val suffix = variant.name.replaceFirstChar(Char::uppercaseChar)
        val minifyTask = "minify${suffix}WithR8"
        val mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        // R8 산출물. AGP 의 공개 artifact 가 아니라 관례적 경로라, 없으면 통과가 아니라 실패다.
        // 같은 폴더에 다른 태스크도 쓰므로 폴더가 아니라 파일만 잡는다.
        val usageFile = layout.buildDirectory.file("outputs/mapping/${variant.name}/usage.txt")
        val configurationFile = layout.buildDirectory.file("outputs/mapping/${variant.name}/configuration.txt")
        val reviewedKeeps = layout.projectDirectory.file("r8-wildcard-keeps.txt")
        val reportDir = layout.buildDirectory.dir("reports/r8-keep-rules/${variant.name}")
        val appNamespace = android.namespace.toString()

        val verify = tasks.register("verify${suffix}KeepRules") {
            group = "verification"
            description = "매니페스트가 이름으로 부르는 클래스가 R8 에 지워졌는지, 그리고 멤버를 지키지 않는 와일드카드 keep 규칙이 새로 들어왔는지 본다."
            dependsOn(minifyTask)
            inputs.file(mergedManifest)
            inputs.file(reviewedKeeps)
            inputs.files(usageFile, configurationFile)
            outputs.dir(reportDir)
            doLast {
                verifyManifestClassesSurvived(
                    manifest = mergedManifest.get().asFile,
                    usage = usageFile.get().asFile,
                    report = File(reportDir.get().asFile, "manifest-classes.txt"),
                    appNamespace = appNamespace,
                )
                verifyWildcardKeepsAreReviewed(
                    configuration = configurationFile.get().asFile,
                    reviewed = reviewedKeeps.asFile,
                    report = File(reportDir.get().asFile, "wildcard-keeps.txt"),
                )
            }
        }
        // matching 으로 걸면 AGP 가 태스크 이름을 바꾸는 날 finalizer 가 조용히 사라져, 릴리스 빌드가
        // 검사 없이 지나간다. named 는 그때 설정 단계에서 바로 터진다.
        afterEvaluate { tasks.named(minifyTask) { finalizedBy(verify) } }
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
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":android:benchmark"))
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


/**
 * R8 가 지운 것의 목록(usage.txt)을 읽어, **매니페스트가 이름으로 가리키는 클래스**가 살아 있는지 본다.
 *
 * 매니페스트에만 이름이 적힌 클래스는 코드에서 참조되지 않으므로, keep 규칙이 멤버를 지키지 않으면
 * R8 이 기본 생성자를 지운다. 그러면 리플렉션 생성이 런타임에 조용히 실패하고, 한참 뒤 엉뚱한 자리에서
 * 터진다(ML Kit 의 ComponentRegistrar, androidx.startup 의 Initializer 가 이 모양이다).
 * 대조할 이름은 런타임이 읽는 그 매니페스트에서 그대로 긁는다 — 목록을 따로 적어 두면 어긋난다.
 */
fun verifyManifestClassesSurvived(manifest: File, usage: File, report: File, appNamespace: String) {
    check(usage.exists()) {
        "R8 의 usage.txt 가 없어 검사할 수 없다: $usage\n" +
            "AGP 가 경로를 옮겼거나 R8 이 돌지 않았다. 둘 다 검사가 꺼진 것이므로 실패로 둔다."
    }

    // android:name / android:value 에 실린 FQCN 꼴 문자열. 값에 접두사가 붙는 경우가 있어
    // (ML Kit 의 레지스트라 키는 `com.google.firebase.components:<FQCN>` 다) 마지막 콜론 뒤를 본다.
    // 권한·액션 이름도 같이 걸리지만 클래스가 아니라 usage.txt 에 없으므로 무해하다 —
    // 거르다가 진짜 클래스를 놓치는 쪽이 더 위험해서 느슨하게 둔다.
    val dollar = "$"
    val attribute = Regex("android:(?:name|value)=\"([^\"]+)\"")
    val looksLikeClass = Regex("^[a-z][\\w.]*(?:\\.[A-Z]\\w*)+(?:\\$dollar\\w+)*$")
    val named = attribute.findAll(manifest.readText())
        .map { it.groupValues[1].substringAfterLast(':') }
        .filter(looksLikeClass::matches)
        .toSortedSet()

    // 앱 자신의 클래스가 하나도 없으면 읽는 쪽이 깨진 것이다. 여기서 멈추지 않으면
    // 아무것도 못 읽은 검사가 조용히 통과한다 — usage.txt 쪽과 같은 이유로 실패로 둔다.
    check(named.any { it.startsWith(appNamespace) }) {
        "매니페스트에서 $appNamespace 의 클래스를 하나도 읽지 못했다. 읽는 규칙이 깨졌다: $manifest"
    }

    val removed = readRemovedByR8(usage)
    val broken = named.mapNotNull { name ->
        when {
            name in removed.classes -> "$name — 클래스 전체가 지워졌다"
            removed.members[name].orEmpty().any { "<init>()" in it } -> "$name — 기본 생성자가 지워졌다"
            else -> null
        }
    }
    check(broken.isEmpty()) {
        buildString {
            appendLine("매니페스트가 이름으로 가리키는 클래스를 R8 이 지웠다. 런타임에 리플렉션 생성이 조용히 실패한다.")
            broken.forEach { appendLine("  - $it") }
            appendLine("그 클래스를 만드는 쪽의 keep 규칙이 멤버를 지키는지 보라. 예:")
            appendLine("  -keep class * implements com.google.firebase.components.ComponentRegistrar { void <init>(); }")
        }
    }

    report.parentFile.mkdirs()
    report.writeText(named.joinToString("\n", postfix = "\n"))
}

/**
 * 멤버를 지키지 않는 와일드카드 keep 규칙을 모아, 사람이 판단해 적어 둔 목록과 대조한다.
 *
 * `-keep class * implements X` 는 R8 full mode 에서 클래스만 남기고 생성자를 지킨다는 보장이 없다.
 * 그 자체로 버그는 아니다 — 생성자를 부르는 코드가 따로 있으면 멀쩡하다. 그래서 자동으로 판정하지
 * 않고, **규칙이 새로 들어오거나 바뀌면 멈춰서 사람이 보게** 한다.
 */
fun verifyWildcardKeepsAreReviewed(configuration: File, reviewed: File, report: File) {
    check(configuration.exists()) {
        "R8 의 configuration.txt 가 없어 검사할 수 없다: $configuration"
    }

    // 라이브러리가 준 규칙이 통째로 합쳐져 있다. 멤버 명세(`{`)가 붙은 줄은 함정이 아니다.
    // 클래스 쪽 와일드카드는 `*` 말고 `io.ktor.client.engine.**` 처럼도 쓰고 `public` 같은 수식어가
    // 붙기도 한다. `-keepnames` 와 `allowshrinking` 은 "살아남으면" 을 전제로 한 다른 관용구라
    // 여기 범위가 아니다.
    val wildcardKeep = Regex(
        "^-keep(?:,[a-z,]+)?\\s+(?:(?:public|final|abstract)\\s+)*(?:class|interface)\\s+" +
            "[\\w.*]*\\*[\\w.*]*\\s+(?:implements|extends)\\s+[\\w.*]+\\s*\$"
    )
    val found = configuration.readLines()
        .map { it.trim() }
        .filter { wildcardKeep.matches(it) && "allowshrinking" !in it }
        .toSortedSet()

    val entries = readReviewedKeeps(reviewed)
    val unjudged = entries.filterValues { notes -> notes.none { it.startsWith(VERDICT_PREFIX) } }.keys
    check(unjudged.isEmpty()) {
        buildString {
            appendLine("${reviewed.name} 의 규칙에 판정이 없다. 규칙 바로 위에 `# $VERDICT_PREFIX` 로 시작하는 줄을 적어라.")
            unjudged.forEach { appendLine("  $it") }
        }
    }

    val added = found - entries.keys
    val gone = entries.keys - found
    check(added.isEmpty() && gone.isEmpty()) {
        buildString {
            appendLine("멤버를 지키지 않는 와일드카드 keep 규칙이 달라졌다. ${reviewed.name} 을 고쳐라.")
            added.forEach { appendLine("  + $it   ← 새로 들어왔다. 해로운지 보고 근거와 함께 추가하라.") }
            gone.forEach { appendLine("  - $it   ← 더 이상 없다. 목록에서 빼라.") }
            appendLine("판단할 때는 R8 설정에서 그 규칙 위의 `# ... section is <라이브러리>` 주석으로 출처를 본다:")
            appendLine("  $configuration")
        }
    }

    report.parentFile.mkdirs()
    report.writeText(found.joinToString("\n", postfix = "\n"))
}

/** 규칙마다 사람이 내린 판정이 있어야 한다. 근거 없이 목록에 올려 두는 것을 막는다. */
private val VERDICT_PREFIX = "판정:"

private class RemovedByR8(val classes: Set<String>, val members: Map<String, List<String>>)

/** usage.txt 는 지워진 클래스를 들여쓰기 없는 줄로, 지워진 멤버를 그 아래 들여쓴 줄로 적는다. */
private fun readRemovedByR8(usage: File): RemovedByR8 {
    val classes = mutableSetOf<String>()
    val members = mutableMapOf<String, MutableList<String>>()
    var current: String? = null
    usage.forEachLine { line ->
        when {
            line.isBlank() -> Unit
            line.first().isWhitespace() -> current?.let { members.getOrPut(it) { mutableListOf() } += line.trim() }
            // 끝에 콜론이 없으면 클래스가 통째로 지워진 것이고, 있으면 아래 멤버만 지워진 것이다.
            else -> current = line.removeSuffix(":").also { if (!line.endsWith(":")) classes += it }
        }
    }
    return RemovedByR8(classes, members)
}

/** 규칙 한 줄과, 그 바로 위에 붙은 주석 줄들(판단 근거)을 짝지어 읽는다. */
private fun readReviewedKeeps(reviewed: File): Map<String, List<String>> {
    check(reviewed.exists()) { "${reviewed.name} 이 없다: $reviewed" }
    val entries = linkedMapOf<String, List<String>>()
    val notes = mutableListOf<String>()
    reviewed.forEachLine { line ->
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> notes.clear()
            trimmed.startsWith("#") -> notes += trimmed.removePrefix("#").trim()
            else -> {
                entries[trimmed] = notes.filter { it.isNotEmpty() }
                notes.clear()
            }
        }
    }
    return entries
}
