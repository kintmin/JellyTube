## Module: android:app

**Role**: App entry point, navigation host, DI assembly, runtime-bound implementations, and **all Android OS-level features**.

This module owns:
- `JellyTubeApplication` — Koin init, app-level setup
- `MainActivity` — edge-to-edge enablement, intent handling, lifecycle
- `MainActivity.handleIntent` — Intent 어댑터(딥링크 문자열 추출 · 공유 파일 수신). 판정과 `NavigationIntent` 디스패치는 `shared:presentation` 의 `navigation/AppViewModel` 이 한다
- `di/AppModule` — Koin module that wires runtime implementations to their interfaces
- `di/WorkerModule` — Worker 바인딩만 따로 (**아래 Rules** 참고)
- `log_impl/` — concrete `AppLog` implementation (Firebase, Logcat, etc.)
- `python_bridge_impl/` — concrete `PythonExecutor` implementation via Chaquopy
- OS-level features (see **OS-level features** below)

> **소스 패키지는 `com.kintmin.jellytube.*` 하나다.** 과거 `:android:platform` 모듈을 합치면서 한동안 `com.kintmin.platform.*` 이 함께 있었지만 지금은 없다. `AndroidManifest.xml` 은 namespace 덕에 `android:name=".service.PlaybackService"` 처럼 상대 표기를 쓴다.

**Rules**:
- Koin 모듈은 `appModule` 하나로 모은다. 새 구현을 추가하면 거기에 건다.
- `workerModule` 만 나눠 둔다. `workerOf` 는 `workManagerFactory()` 가 쓰는 별도 해석 경로라 `single` 과 성격이 다르다.

---

## What belongs here

### Runtime implementations

Implementations that are **bound to the Android runtime**:

- **Log implementation** (`log_impl/AppLogImpl`): The `shared:log` module defines only the API. The concrete implementation (Firebase Analytics, Crashlytics, Logcat) lives here. Register it in `di/AppModule`.
- **Python bridge implementation** (`python_bridge_impl/PythonExecutorImpl`): Chaquopy is an Android build-time dependency; the implementation belongs here. Register it in `di/AppModule`.

### OS-level features

Any code that directly uses Android OS APIs for background execution, inter-process communication, or system events:

- `Service` subclasses (foreground or bound) → `service/`
- `BroadcastReceiver` subclasses → `receiver/`
- `Worker` / `CoroutineWorker` subclasses → `worker/`, worker DI 는 `di/WorkerModule.kt`
- `NotificationChannel` creation, `NotificationCompat.Builder` usage → `push_notification/`
- `MediaSession`, `MediaController`, `MediaBrowser` bindings → `service_controller/`
- 딥링크 주소를 `Uri` 로 바꾸는 어댑터 → `deeplink/` (**상수·조립은 `shared:presentation` 소유**)

### Adapter — port 의 Android 구현

`shared:presentation`이 `com.kintmin.presentation.port`에 **인터페이스만** 선언하고, 구현체는 이 모듈에 둔다. `presentation`은 CMP(KMP) 모듈이라 Android API를 직접 쓸 수 없고, `app`은 이미 `presentation`을 의존하므로 순환이 생기지 않는다.

현재 포트와 adapter:

| 포트 (`presentation/port/`) | Android adapter | iOS adapter |
|---|---|---|
| `PlaybackController` | `service_controller/PlaybackControllerImpl` | `adapter/IosPlaybackController`(Swift 엔진 브릿지) |
| `AudioPreviewPlayer` | `audio_preview/AudioPreviewPlayerImpl` | 미지원 스텁(음원 편집 제외) |
| `StepMeasurementController` | `service_controller/StepMeasurementControllerImpl` | `IosStepMeasurementController`(CMPedometer) |
| `FileShareServerController` | `service_controller/FileShareServerControllerImpl`(UseCase 로 서버를 켜고 끄고, `FileShareForegroundService` 로 프로세스를 붙잡는다) | `IosFileShareServerController`(UseCase 로 서버를 켜고 끈다. 포그라운드 서비스가 없어 앱이 서스펜드되면 멈춘다) |
| `StartYoutubeDownload` | `worker/starter/StartYoutubeDownloadImpl` | `IosStartYoutubeDownload`(추출 + background URLSession) |
| `StartLyricsVariantCreation` | `worker/starter/StartLyricsVariantCreationImpl` | `IosStartLyricsVariantCreation`(앱 수명 스코프 + 로컬 알림) |
| `StartAnomalyDataCheck` | `worker/starter/StartAnomalyDataCheckImpl` | `IosStartAnomalyDataCheck`(앱 수명 스코프 + 로컬 알림) |
| `StartAudioEditSave` | `worker/starter/StartAudioEditSaveImpl` | 미지원 스텁(음원 편집 제외) |
| `DataMigrationSendController` | `service_controller/DataMigrationSendControllerImpl`(UseCase 로 이전 세션을 열고 닫는다. PC 수신과 같은 서버를 쓴다) | 미지원 스텁(보내는 서버가 Android 에만 있다) |
| `RunningTaskMonitor` | `worker/RunningTaskMonitorImpl`(WorkManager 태그·고유 이름 + 편집 저장 + PC 수신) | `IosRunningTaskMonitor`(실행부가 세는 `task/IosRunningTaskTracker` + 편집 저장 + PC 수신) |
| `MigrationSourceDiscovery` | `data_migration/UnsupportedMigrationSourceDiscovery`(Android 는 보내는 쪽만) | `adapter/IosMigrationSourceDiscovery`(Swift `NWBrowser` 브릿지. 목록 탐색 → 고른 기기만 주소 풀기) |

iOS adapter 의 자리는 **`ios/shared` 의 `adapter/`** 다(이 모듈의 짝인 iOS 조립 모듈). `adapter/IosAdapterModule.kt` 가 바인딩한다.

### port 와 adapter — 말을 섞지 않는다

- **port 는 인터페이스다.** 코어가 바깥을 향해 뚫은 구멍이고, 선언 자리는 그 구멍이 필요한
  모듈이다 — `shared:presentation` 의 `port/`(11개), `shared:domain`(`ScheduleDailyStepClosing` ·
  `ReceiveKeepAlive`), `shared:log`(`AppLog`), `shared:data`(`PythonExecutor` ·
  `LyricsTransliterator`). **네 모듈에 흩어져 있는 것이 정상이다** — 각 레이어가 자기 구멍을 뚫는다.
- **adapter 는 그 구멍에 끼우는 플랫폼 구현이다.** 이 모듈과 `ios/shared/adapter/` 에 있는 것이
  전부 adapter다. "포트 구현" 보다 adapter 가 정확하다.
- **port 이름에 플랫폼 기구를 넣지 않는다.** `ExecuteYoutubeDownload`(WorkManager enqueue) ·
  `RunningWorkMonitor`(WorkManager) · `MediaControllerManager`(media3) 가 그래서 `Start~` ·
  `RunningTaskMonitor` · `PlaybackController` 로 바뀌었다. Android 기구 모양으로 구멍을 뚫으면
  그 기구가 없는 플랫폼이 흉내를 내야 한다 — iOS 가 WorkManager 를 대신하려고
  `IosRunningTaskTracker` 를 직접 만든 것이 그 흔적이다.
- `worker/starter/` 는 **UseCase 가 아니다.** Worker 를 enqueue 하는 adapter다.
  `shared:domain` 의 UseCase 와 헷갈리지 않게 이름을 가른다.

**Rules**:
- 새 포트가 필요하면 인터페이스는 `presentation/port/`에, adapter는 여기에 두고 `di/AppModule.kt`에 `single<Port> { PortImpl(androidContext()) }`로 등록한다.
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

### 걸음수

`StepSensorProcessor` 는 Android 걸음 센서(부팅 뒤 누적값, 재부팅으로 0 부터 다시 셈)에 맞춘 계산이라 이 모듈에 둔다. iOS 는 CMPedometer 가 그날 누적을 바로 주어 같은 계산이 필요 없다. 두 플랫폼이 같이 써야 하는 것은 차트 한 칸(`shared:domain` 의 `StepSlot`)뿐이다.

### 파일공유 서버

`FileShareForegroundService` 는 **프로세스를 살려 두는 일만** 한다(포그라운드 알림, 데이터 보내기 중 CPU·Wi-Fi 잠금). 서버·광고·경로·이전 세션은 `shared:data` 의 `file_share/` 가 가지며, 규칙은 `shared/data/AGENTS.md` §파일 공유 서버가 단일 진원지다.

- **이 모듈은 Ktor 에 의존하지 않는다.** 서버를 켜고 끄는 것은 컨트롤러가 UseCase 로 한다. 서비스가 알림의 중지나 OS 에 의해 내려가면 `onDestroy` 에서 UseCase 로 서버도 내린다.

- Every foreground service must call `startForeground()` with a valid notification before Android OS enforces it.
- Services that can be started/stopped externally expose a `companion object` with `startService(context)` / `stopService(context)` static helpers (see `StepForegroundService` for reference).
- **`shared:presentation`에서 서비스를 직접 시작하지 않는다.** Composable이 `startService(context)`를 부르는 구조는 금지다. `presentation/port/`의 컨트롤러 포트(예: `StepMeasurementController`)를 ViewModel에 주입해 거기서 제어한다.
- `MainActivity`는 이 모듈 소속이므로 서비스 companion을 직접 불러도 된다.

---

## WorkManager Workers

- Workers live under `worker/`. Worker 바인딩은 `di/WorkerModule.kt` 가 가진다.
- Workers must not hold references to UI components.
- Worker enqueue 는 `worker/starter/`의 `Start~Impl`이 담당한다(UseCase 가 아니라 adapter다). ViewModel은 `presentation/port/`의 인터페이스만 본다.
- 읽는 쪽인 `worker/RunningTaskMonitorImpl` 도 같은 WorkManager 태그·고유 이름을 쓴다. 상수는 `Start~Impl` 의 companion 이 단일 진원지다.

---

## BroadcastReceivers

- Receivers live under `receiver/`.
- Receivers must be lightweight: do the minimum work, then delegate to a Service or Worker for anything long-running.
- Register receivers that need runtime registration via the DI module.

---

## Deep Link Handling — 진입 통로만 이 모듈이다

**Flow**:
1. `MainActivity.onNewIntent()` / `onCreate()` → `MainActivity.handleIntent(intent)`
2. `intent.data` 를 꺼내 **문자열로** `AppViewModel.handleDeepLink(url)` 에 넘긴다(공유 파일은 `importSharedAudio`)
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
