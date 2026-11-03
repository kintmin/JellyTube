## Module: android:app

**Role**: App entry point, navigation host, DI assembly, runtime-bound implementations, and **all Android OS-level features**.

This module owns:
- `JellyTubeApplication` — Koin init, app-level setup
- `MainActivity` — edge-to-edge enablement, intent handling, lifecycle
- `MainNavHost` — top-level Compose navigation graph wiring
- `MainViewModel` — deep link parsing, shared file handling, navigation intent dispatch
- `NavigationIntent` — sealed class of all top-level navigation commands
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
- Deep link URI constants and URI builder helpers → `platform/deeplink/`

### Port implementations (`presentation`의 포트 구현)

`shared:presentation`이 `com.kintmin.presentation.port`에 **인터페이스만** 선언하고, 구현체는 이 모듈에 둔다. `presentation`은 CMP(KMP) 모듈이라 Android API를 직접 쓸 수 없고, `app`은 이미 `presentation`을 의존하므로 순환이 생기지 않는다.

현재 포트와 구현체:

| 포트 (`presentation/port/`) | 구현체 (`com.kintmin.platform.*`) |
|---|---|
| `MediaControllerManager` | `service_controller/MediaControllerManagerImpl` |
| `AudioPreviewPlayer` | `audio_preview/AudioPreviewPlayerImpl` |
| `StepMeasurementController` | `service_controller/StepMeasurementControllerImpl` |
| `FileShareServerController` | `service_controller/FileShareServerControllerImpl` |
| `ExecuteYoutubeDownload` | `worker/usecase/ExecuteYoutubeDownloadImpl` |
| `ExecuteLyricsVariantCreation` | `worker/usecase/ExecuteLyricsVariantCreationImpl` |
| `ExecuteAnomalyDataCheck` | `worker/usecase/ExecuteAnomalyDataCheckImpl` |
| `ExecuteAudioEditSave` | `worker/usecase/ExecuteAudioEditSaveImpl` |

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

## DeepLinkConstants

`DeepLinkConstants` (package `com.kintmin.platform.deeplink`) is the single source of truth for all deep link paths, query keys, and URI builders.

**Rules**:
- All path segment strings must be declared as `const val` inside `DeepLinkConstants.Path`.
- All query parameter key strings must be declared as `const val` inside `DeepLinkConstants.QueryKey`.
- URI pattern strings belong in the private `UriPattern` object.
- Public URI builders belong in `UriBuilder` object and must return `android.net.Uri`.
- **Do not** hardcode URI strings outside of `DeepLinkConstants`.
- `DEEP_LINK_HOST` is `www.jellytube.com`. Changing it requires updating the manifest `intent-filter` as well.

---

## Services

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

## Deep Link Handling

**Flow**:
1. `MainActivity.onNewIntent()` / `onCreate()` calls `MainViewModel.handleIntent(intent)`
2. `MainViewModel` reads `intent.data` (Uri) and delegates to `sendNavigationIntents(uri)`
3. `sendNavigationIntents` uses `DeepLinkConstants.Path` and `DeepLinkConstants.QueryKey` to parse segments
4. Navigation commands are sent as `NavigationIntent` via `navigationIntentChannel` (capacity=8 Channel)
5. `MainNavHost` collects `navigationIntentFlow` and calls the appropriate `NavController` functions

**Rules**:
- Adding a new deep link now changes only this module: add the path constant to `DeepLinkConstants`, add the branch in `sendNavigationIntents`, add the `NavigationIntent` entry, wire it in `MainNavHost`, and update `AndroidManifest.xml` `intent-filter` if the link must be interceptable from outside the app.
- Always consume the URI after processing: `intent.data = null`
- Always send `NavigationIntent.PopAll` before navigating to a new deep link destination.

---

## NavigationIntent Rules

- `NavigationIntent` is a sealed class in this module.
- Add new entries here when a new navigation destination is needed from `MainViewModel`.
- `MainNavHost` is the only consumer of `navigationIntentFlow`. Do not collect it elsewhere.
