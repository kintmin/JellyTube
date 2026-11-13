## Module: android:app

**Role**: App entry point, navigation host, DI assembly, runtime-bound implementations, and **all Android OS-level features**.

This module owns:
- `JellyTubeApplication` — Koin init, app-level setup
- `MainActivity` — edge-to-edge enablement, intent handling, lifecycle
- `MainViewModel` — Intent 어댑터(딥링크 문자열 추출 · 공유 파일 수신) + `NavigationIntent` 디스패치
- `di/AppModule` — Koin module that wires runtime implementations to their interfaces
- `log_impl/` — concrete `AppLog` implementation (Firebase, Logcat, etc.)
- `python_bridge_impl/` — concrete `PythonExecutor` implementation via Chaquopy
- `com.kintmin.platform.*` — OS-level features (see **OS-level features** below)

> **패키지 두 개가 공존한다.** `com.kintmin.jellytube.*`(진입점/네비/DI)와 `com.kintmin.platform.*`(OS 기능). 후자는 과거 `:android:platform` 모듈이었고, CMP 전환 과정에서 이 모듈로 합쳤다. 패키지명을 유지한 이유는 `AndroidManifest.xml`이 Service/Receiver를 FQCN으로 선언하고 있어서다. namespace(`com.kintmin.jellytube`)와 소스 패키지가 다른 것은 적법하다.

---

## What belongs here

### Runtime implementations

Implementations that are **bound to the Android runtime**:

- **Log implementation** (`log_impl/AppLogImpl`): The `shared:log` module defines only the API. The concrete implementation (Firebase Analytics, Crashlytics, Logcat) lives here. Register it in `di/AppModule`.
- **Python bridge implementation** (`python_bridge_impl/PythonExecutorImpl`): Chaquopy is an Android build-time dependency; the implementation belongs here. Register it in `di/AppModule`.

### OS-level features (`com.kintmin.platform.*`)

Any code that directly uses Android OS APIs for background execution, inter-process communication, or system events:

- `Service` subclasses (foreground or bound) → `platform/service/`
- `BroadcastReceiver` subclasses → `platform/receiver/`
- `Worker` / `CoroutineWorker` subclasses → `platform/worker/`, worker DI in `platform/worker/di/`
- `NotificationChannel` creation, `NotificationCompat.Builder` usage → `platform/push_notification/`
- `MediaSession`, `MediaController`, `MediaBrowser` bindings → `platform/service_controller/`
- 딥링크 주소를 `Uri` 로 바꾸는 어댑터 → `platform/deeplink/` (**상수·조립은 `shared:presentation` 소유**)

### Port implementations (`presentation`의 포트 구현)

`shared:presentation`이 `com.kintmin.presentation.port`에 **인터페이스만** 선언하고, 구현체는 이 모듈에 둔다. `presentation`은 CMP(KMP) 모듈이라 Android API를 직접 쓸 수 없고, `app`은 이미 `presentation`을 의존하므로 순환이 생기지 않는다.

현재 포트와 구현체:

| 포트 (`presentation/port/`) | Android 구현 (`com.kintmin.platform.*`) | iOS 구현 |
|---|---|---|
| `MediaControllerManager` | `service_controller/MediaControllerManagerImpl` | `ios/shared` 의 `media/IosMediaControllerManager`(Swift 엔진 브릿지) |
| `AudioPreviewPlayer` | `audio_preview/AudioPreviewPlayerImpl` | 미지원 스텁(음원 편집 제외) |
| `StepMeasurementController` | `service_controller/StepMeasurementControllerImpl` | `IosStepMeasurementController`(CMPedometer) |
| `FileShareServerController` | `service_controller/FileShareServerControllerImpl`(UseCase 로 서버를 켜고 끄고, `FileShareForegroundService` 로 프로세스를 붙잡는다) | `IosFileShareServerController`(UseCase 로 서버를 켜고 끈다. 포그라운드 서비스가 없어 앱이 서스펜드되면 멈춘다) |
| `ExecuteYoutubeDownload` | `worker/usecase/ExecuteYoutubeDownloadImpl` | `IosExecuteYoutubeDownload`(추출 + background URLSession) |
| `ExecuteLyricsVariantCreation` | `worker/usecase/ExecuteLyricsVariantCreationImpl` | `IosExecuteLyricsVariantCreation`(앱 수명 스코프 + 로컬 알림) |
| `ExecuteAnomalyDataCheck` | `worker/usecase/ExecuteAnomalyDataCheckImpl` | `IosExecuteAnomalyDataCheck`(앱 수명 스코프 + 로컬 알림) |
| `ExecuteAudioEditSave` | `worker/usecase/ExecuteAudioEditSaveImpl` | 미지원 스텁(음원 편집 제외) |
| `DataMigrationSendController` | `service_controller/DataMigrationSendControllerImpl`(UseCase 로 이전 세션을 열고 닫는다. PC 수신과 같은 서버를 쓴다) | 미지원 스텁(보내는 서버가 Android 에만 있다) |
| `RunningWorkMonitor` | `running_work/RunningWorkMonitorImpl`(WorkManager 태그·고유 이름 + 편집 저장 + PC 수신) | `IosRunningWorkMonitor`(실행부가 세는 `IosRunningWorkTracker` + 편집 저장 + PC 수신) |
| `MigrationSourceDiscovery` | `data_migration/UnsupportedMigrationSourceDiscovery`(Android 는 보내는 쪽만) | `ios/shared` 의 `migration/IosMigrationSourceDiscovery`(Swift `NWBrowser` 브릿지. 목록 탐색 → 고른 기기만 주소 풀기) |

iOS 구현의 자리는 **`shared:presentation` 의 `iosMain/port/`** 다(`MediaControllerManager`·`MigrationSourceDiscovery` 는 예외 — Swift 브릿지에 기댄다). 이유는 `shared/presentation/AGENTS.md` §포트 와 `ios/app/AGENTS.md` 에 있다.

**Rules**:
- 새 포트가 필요하면 인터페이스는 `presentation/port/`에, 구현체는 여기에 두고 `platform/di/PlatformModule.kt`에 `single<Port> { PortImpl(androidContext()) }`로 등록한다.
- **인자 전달로 해결되면 포트를 만들지 않는다.** 값 하나를 넘기면 되는 경우(예: `isDebugTabVisible`)는 Screen/NavGraph 파라미터로 준다. 포트는 ViewModel이 주입받아야 하거나 `Flow`를 노출해야 할 때만 만든다.
- 도메인 규칙은 포트가 아니다. 순수 판정 로직은 `shared:domain`에 둔다.

---

## What does NOT belong here

- Business logic → `shared:domain`
- Repository implementations → `shared:data`
- UI composables / ViewModel → `shared:presentation`

---

## DeepLinkConstants — 어댑터일 뿐이다

경로·쿼리 키·주소 조립의 단일 진원지는 **`shared:presentation` 의
`com.kintmin.presentation.deeplink.DeepLinkConstants`** 다. 이 모듈의 동명 객체는
`PendingIntent` 가 `android.net.Uri` 를 요구해서 존재한다.

**Rules**:
- **주소 문자열을 여기서 조립하지 않는다.** 공통 `UrlBuilder` 의 결과를 `toUri()` 로만 바꾼다.
- 새 path/query 키는 공통 쪽 `Path`/`QueryKey` 에 더한다. 이 모듈에 복제하지 않는다.
- `DEEP_LINK_HOST`(= 공통 `HOST`, `www.jellytube.com`) 변경 시 manifest `intent-filter` 도 함께 바꾼다.

---

## Services

### 파일공유 서버

`FileShareForegroundService` 는 **프로세스를 살려 두는 일만** 한다(포그라운드 알림, 데이터 보내기 중 CPU·Wi-Fi 잠금). 서버·광고·경로·이전 세션은 `shared:data` 의 `file_share/` 가 가지며, 규칙은 `shared/data/AGENTS.md` §파일 공유 서버가 단일 진원지다.

- **이 모듈은 Ktor 에 의존하지 않는다.** 서버를 켜고 끄는 것은 컨트롤러가 UseCase 로 한다. 서비스가 알림의 중지나 OS 에 의해 내려가면 `onDestroy` 에서 UseCase 로 서버도 내린다.

- Every foreground service must call `startForeground()` with a valid notification before Android OS enforces it.
- Services that can be started/stopped externally expose a `companion object` with `startService(context)` / `stopService(context)` static helpers (see `StepForegroundService` for reference).
- **`shared:presentation`에서 서비스를 직접 시작하지 않는다.** Composable이 `startService(context)`를 부르는 구조는 금지다. `presentation/port/`의 컨트롤러 포트(예: `StepMeasurementController`)를 ViewModel에 주입해 거기서 제어한다.
- `MainActivity`는 이 모듈 소속이므로 서비스 companion을 직접 불러도 된다.

---

## WorkManager Workers

- Workers live under `platform/worker/`. Worker-specific DI modules live under `platform/worker/di/`.
- Workers must not hold references to UI components.
- Worker enqueue 는 `platform/worker/usecase/`의 `Execute~Impl`이 담당한다. ViewModel은 `presentation/port/`의 인터페이스만 본다.

---

## BroadcastReceivers

- Receivers live under `platform/receiver/`.
- Receivers must be lightweight: do the minimum work, then delegate to a Service or Worker for anything long-running.
- Register receivers that need runtime registration via the DI module.

---

## Deep Link Handling — 진입 통로만 이 모듈이다

**Flow**:
1. `MainActivity.onNewIntent()` / `onCreate()` → `MainViewModel.handleIntent(intent)`
2. `MainViewModel` 이 `intent.data` 를 꺼내 **문자열로** `onDeepLink(url)` 에 넘긴다
3. 판정과 명령 생성은 `shared:presentation` 의 `DeepLinkParser.parse(url, 재생상태)` 가 한다
4. 결과 `NavigationIntent` 들을 `navigationIntentChannel`(capacity=8)로 보낸다
5. 공통 `MainNavHost` 가 `navigationIntentFlow` 를 수집해 `NavController` 를 부른다

**Rules**:
- **파싱 분기를 이 모듈에 두지 않는다.** 두 플랫폼이 같은 답을 내야 한다. 새 목적지 추가
  절차는 `.claude/skills/add-deep-link` 가 단일 진원지다.
- 처리 후 URI 를 소비한다: `intent.data = null`
- `PopAll` 은 파서가 뿌리 경로 확인 직후 넣는다 — 호출부에서 따로 보내지 않는다.

---

## NavigationIntent Rules

- `NavigationIntent` 는 **`shared:presentation` 의 `navigation/`** 에 있다. 이 모듈에 두지 않는다.
- `MainNavHost` 가 `navigationIntentFlow` 의 유일한 소비자다. 다른 곳에서 collect 금지.
