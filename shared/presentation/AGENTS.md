## Module: shared:presentation

**Role**: Compose Multiplatform UI and UI state management only. No business logic. No OS-level code.

---

## 소스셋 규약 (CMP)

| 소스셋 | 무엇을 두는가 |
|---|---|
| `src/commonMain/kotlin` | **기본 자리.** 화면/ViewModel/Intent/UiState/Event/Navigation/theme/extension |
| `src/androidMain/kotlin` | `expect` 선언의 Android `actual`, 그리고 공통화가 불가능해 의도적으로 Android 전용으로 남긴 코드 |
| `src/androidHostTest/kotlin` | 단위테스트. **`commonTest`를 만들지 않는다** — mockk 가 JVM 전용이라 iOS 컴파일이 깨진다 |
| `src/iosMain/kotlin` | (아직 없음) iOS `actual`과 iOS 전용 화면 |

**iOS 타겟은 선언돼 있으나 Windows 에서는 컴파일 태스크가 돌지 않는다.** 따라서 `commonMain` 에 Android/JVM 전용 API 를 넣어도 **컴파일러가 잡아주지 않는다.** 아래 순수성 게이트를 승격 작업의 통과 기준으로 쓴다.

```powershell
rg -n --glob "shared/presentation/src/commonMain/**/*.kt" `
  "^import (android\.|java\.|javax\.|androidx\.(activity|core|palette|work|webkit|compose\.ui\.viewinterop)\.)|^import androidx\.compose\.ui\.platform\.Local(Context|View|Configuration)|^import androidx\.lifecycle\.(lifecycleScope|repeatOnLifecycle)|^import org\.koin\.androidx|\.format\(|[^.\w]Math\.|System\.currentTimeMillis\("
```

`^import` 로 묶은 항목은 import 줄만, 나머지는 호출 형태(`.format(` / `System.currentTimeMillis(`)만 집는다. KDoc 에 API 이름을 적는 경우를 오탐하지 않기 위해서다 — 그러니 **주석에도 호출 형태를 적지 않는다.**

> **구멍 2개를 메운 기록**: iOS 를 처음 실제로 컴파일했을 때 이 게이트가 0줄을 냈는데도 `commonMain` 4파일이 깨졌다. ① `LocalConfiguration` 이 `Local(Context|View)` 에 없었고, ② 확장 호출 형태(`"%,d"` 뒤에 붙는 서식 확장)가 `String.format(` 패턴에 걸리지 않았다. 공통 코드에 `format(` 을 호출할 정당한 자리가 없으므로 뒤쪽을 `\.format\(` 로 넓혔다.

결과가 **0줄**이어야 한다. `androidx.compose.*` / `androidx.navigation.*` / `androidx.lifecycle.compose.*` 는 CMP·JetBrains 포크가 같은 패키지명을 쓰므로 **허용**이다. 위 정규식은 Android 전용 하위 패키지와 심볼만 집어서 금지한다.

### 승격 현황 — 전환 완료

| 소스셋 | 파일 수 | 내용 |
|---|---|---|
| `commonMain` | **212** | 화면·ViewModel·Intent·UiState·Event·Navigation·theme·extension·port 전부 |
| `androidMain` | 12 | `platform/` actual 9 + 공통화 불가 3 |
| `iosMain` | 9 | `platform/` actual 9 |

**화면 코드는 212 / 215(98.6%)가 `commonMain`** 이다. `androidMain` 에 남은 화면 코드 3개는 **의도적 잔류**이며 해소할 생각이 없다.

| 파일 | 왜 공통화하지 않는가 |
|---|---|
| `ui/custom_ui/image_drawing/ImageDrawingView.kt` | S펜 측면 버튼(`MotionEvent.BUTTON_STYLUS_PRIMARY`)으로 지우개를 판정한다. CMP common `PointerType` 에는 `Stylus`/`Eraser` 만 있고 버튼 상태에 대응하는 API 가 없다. Apple Pencil 에는 그 버튼이 아예 없다 |
| `ui/custom_ui/floating_component/FloatingComponentEditor.kt` | `Modifier.pointerInteropFilter` + `MotionEvent` 로 회전 제스처의 각도를 추적한다 |
| `ui/custom_ui/floating_component/FloatingComponentView.kt` | 위 파일의 `EditableFloatingComponent` 를 쓴다 |

`expect` 를 만들면 iOS actual 이 영구히 죽은 코드가 된다(루트 `AGENTS.md` §2 "단일 사용처에 추상화 금지"). iOS 에서 이 기능을 어떻게 다룰지는 Tier3 화면 분기와 함께 결정한다.

> **`~Navigation.kt` 연쇄에 대한 기록**: 이 파일은 `@Serializable` Route 와 `NavGraphBuilder` 등록을 한 파일에 담는다(프로젝트 규약). 등록부가 Screen 을 참조하므로 **Screen 이 `androidMain` 이면 Route 도, 그 Route 를 `toRoute()` 로 읽는 ViewModel 도 따라 내려간다.** 승격 중에는 이 연쇄가 반복해서 걸림돌이었지만, Screen 이 전부 올라가면서 자연히 해소됐다. 파일을 쪼개 Route 만 올리는 선택지를 쓰지 않은 것이 결과적으로 맞았다.


## 플랫폼 탈출구

### 분기 기준은 "구현이 어느 모듈에 있나" 하나다

| 구현 위치 | 방식 | 왜 |
|---|---|---|
| `presentation` 안의 `androidMain`/`iosMain` | **`expect`/`actual`** (`platform/` 패키지) | 컴파일 타임에 actual 누락을 잡는다. Koin 은 런타임에 터진다 |
| **다른 모듈**(`android:app`) | **`interface` + Koin** (`port/` 패키지) | `expect`/`actual` 은 한 모듈 안의 소스셋 분기라 다른 모듈의 구현에 닿을 수 없다 |

- Compose 로컬(`LocalContext` 등)이 필요하면 `@Composable expect fun remember~()` 로 생성만 `expect` 한다.
- **`expect class` 는 쓰지 않는다.** 공통 테스트에서 인스턴스를 만들 수 없어 그 타입을 쓰는 로직이 통째로 테스트 불가가 된다. 계약은 `interface`, 생성만 `expect`.
- **커스텀 `CompositionLocal` 은 만들지 않는다.** 위 두 가지로 충분하고, 세 번째 경로가 생기면 "이건 어느 쪽이냐"를 매번 따져야 한다.

### 현재 탈출구 목록

| 탈출구 | 방식 | 쓰는 곳 |
|---|---|---|
| `platform/PlatformFullScreenDialog(onDismissRequest, hideSystemBars, content)` | `expect`/`actual`. Android 는 창 배경·DIM·decor 와 `WindowInsetsControllerCompat` 를 직접 만진다(`DialogProperties.decorFitsSystemWindows` 는 Android 전용). iOS 는 `Dialog(usePlatformDefaultWidth = false)` | `JellyTubeDialog`(호출부 22곳, 공개 시그니처 불변), `FullScreenImageViewer`(`hideSystemBars = true`) |
| `platform/PlatformBottomSheetSystemBarEffect(lightNavigationBarIcons)` | `expect`/`actual`. Android 만 실동작, iOS no-op(내비게이션바가 없다) | 바텀시트 3종 |
| `platform/Toaster` + `@Composable expect fun rememberToaster()` | `interface` + `expect`/`actual`. Android actual 이 `LocalContext` 를 집어 만든다. iOS actual 은 메시지를 로그로만 남긴다 | Composable 문맥 Toast 10곳. MVI `Event.ShowToast` 계층은 순수 Kotlin 이라 무변경 |
| `platform/rememberAudioFilePicker(startAtAudioRoot, onPicked)` | `expect`/`actual`. Android actual 이 `ActivityResultContracts.OpenMultipleDocuments` 와 "오디오" 루트용 커스텀 contract(`DocumentsContract.EXTRA_INITIAL_URI`)를 품는다. iOS 는 `UIDocumentPickerViewController` 가 필요해 아직 no-op | `MainScreen`, `SettingScreen`(`startAtAudioRoot = true`), `SettingShareScreen` |
| `platform/PermissionController` + `rememberPermissionController()` | `interface` + `expect`/`actual`. **원시 권한 문자열을 노출하지 않는다** — `AppPermission` enum(`Notification`/`MediaStorage`/`ActivityRecognition`)만 받고, OS 버전별로 어떤 권한이 실제 필요한지는 actual 이 판단한다(저장: SDK 28 이하 WRITE, 32 이하 READ, 그 위 불필요). iOS 는 권한 모델이 달라 "필요 없음"으로 답한다 | `MainScreen`(시작 시 일괄 요청), `SettingScreen`(신체 활동 인식), `SettingDeviceSaveSelectScreen`(저장) |
| `platform/rememberAppVersionName()` | `expect`/`actual`. Android 는 `PackageManager`(SDK 33 분기), iOS 는 `NSBundle` 의 `CFBundleShortVersionString` | `SettingScreen` |
| `platform/extractDominantColor(imageFileFullPath)` | `expect`/`actual` **suspend 함수**(Compose 로컬이 필요 없다). Android 는 `BitmapFactory` 2패스 다운샘플 + `Palette`, iOS 는 아직 null. `Dispatchers.IO` 가 JVM 전용이라 디스패처 선택도 actual 이 한다. 호출자는 null 이면 기본 그라데이션으로 떨어진다 | `PlayerDetailScreen` |
| `platform/AppLogSharer` + `rememberAppLogSharer()` | `fun interface` + `expect`/`actual`. Android 는 `filesDir/app_logs` 의 `.log` 를 모아 FileProvider URI 로 `ACTION_SEND_MULTIPLE` chooser 를 띄운다. iOS 는 `UIActivityViewController` 가 필요해 아직 `Failed`. 결과는 `AppLogShareResult` enum 으로 돌려주고 **안내 문구는 호출자가 정한다** | `AppLogScreen` |
| `platform/SystemSettingsOpener` + `rememberSystemSettingsOpener()` | `fun interface` + `expect`/`actual`. 목적지를 `SystemSettingsTarget`(`Alarm`/`Sound`) enum 으로 받고 인텐트 폴백 체인은 actual 이 품는다(SHOW_ALARMS 미지원 기기 → SET_ALARM). iOS 는 시스템 설정의 특정 화면으로 보낼 수 없어 항상 false | `SettingDeviceSaveScreen` |
| `platform/WebViewHandle` + `rememberWebViewHandle()` + `PlatformWebView(...)` | `@Stable interface` + `expect`/`actual`. **플랫폼 뷰 객체를 노출하지 않는다** — 노출하면 그 타입이 시그니처를 타고 호출자까지 전염된다(실제로 `MainScreen` 이 `WebView` 를 들고 있었다). 핸들이 생명주기(`stopLoading`/`removeView`/`destroy`)와 생성 실패 상태까지 소유한다. Android actual 이 `AndroidView` + `WebViewClient` 스킴 라우팅(`intent://` 의 `browser_fallback_url` 폴백 포함) + 쿠키·Referer 설정을 품는다. iOS 는 `WKWebView` + `UIKitView` 가 필요해 아직 안내 문구만 보여준다 | `MainScreen`, `YoutubeWebView` |

`port/` 의 포트 9개(`MediaControllerManager`, `AudioPreviewPlayer`, `Execute*` 4종, `StepMeasurementController`, `FileShareServerController`)는 후자이며 `android:app` 의 `platform/di/PlatformModule.kt` 가 바인딩한다. 배치 규칙은 `android/app/AGENTS.md` 의 **Port implementations** 절이 단일 진원지다.

### 추상화 없이 치환만으로 끝낸 것

- Coil 13파일: `LocalContext.current` → `coil3.compose.LocalPlatformContext.current`, `File(path)` → `path.toPath()`(okio). 경로에 공백·`#` 이 있으면 깨지는 `"file://$path"` 문자열 방식은 쓰지 않았다.
- 폴링 3파일: `repeatOnLifecycle` + `lifecycleScope.launch` → `LifecycleResumeEffect` + `onPauseOrDispose { job.cancel() }`. **원래 코드는 RESUMED 를 벗어나도 루프가 취소되지 않는 버그였고 이 치환으로 함께 고쳐졌다.**
- 뒤로가기 3곳: `androidx.activity.compose.BackHandler` → `androidx.navigationevent.compose.NavigationBackHandler`(`state = rememberNavigationEventState(NavigationEventInfo.None)`, `isBackEnabled = ...`). CMP 1.12 에서 `ui-backhandler` 가 deprecated 되어 이쪽이 공통 경로다. `YoutubeWebView` 의 `BackHandler` 는 그 파일이 `androidMain` 잔류라 손대지 않았다.
- 유튜브 URL 판정: `androidx.core.net.toUri` → `shared:domain` 의 `YoutubeUrl`(순수 문자열 파싱 + 단위테스트 6개). 플랫폼 URI 파서는 관용적이라 판정 결과가 플랫폼마다 달라질 수 있다.

**비-Composable Android 헬퍼 안의 Toast 5곳은 그대로 뒀다** — `AppLogScreen.sendAppLogByEmail`, `SettingDeviceSaveScreen.startFirstAvailableActivity`. 그 함수 자체가 `androidMain` 잔류라 추상화할 이유가 없다.


### 미결 사항 — iOS framework 구성

iOS framework 는 현재 `shared/data/build.gradle.kts` 에만 있다(`baseName = "shared"`, `export(domain)`, `export(log)`). SKIE 도 거기만 달려 있다. presentation 을 iOS 에 노출하려면 umbrella 모듈 신설 등 선택이 필요한데, **Windows 에서 iOS 링크 태스크가 돌지 않아 무엇이 되는지 검증할 수 없다.** 추측으로 배선하지 않는다. macOS 를 확보한 뒤 결정한다.

---

## 포트 (`port/`)

Android/iOS 런타임 기능은 이 모듈이 **인터페이스만** 선언하고 구현체는 `android:app` 에 둔다. 배치 규칙과 현재 포트 목록은 `android/app/AGENTS.md` 의 **Port implementations** 절이 단일 진원지다.

- **인자 전달로 해결되면 포트를 만들지 않는다.** 값 하나를 넘기면 되는 경우(예: `isDebugTabVisible`)는 Screen/NavGraph 파라미터로 받는다.
- 순수 판정 로직은 포트가 아니다. `shared:domain` 에 둔다.

---

## Architecture: MVI

Every screen follows a strict MVI structure. Each screen feature lives in its own package and contains exactly these files:

| File | Responsibility |
|---|---|
| `~Screen.kt` | Composable screen entry point + stateful wrapper that wires ViewModel |
| `~ViewModel.kt` | Holds `uiState: StateFlow<~UiState>` and `eventFlow: Flow<~Event>`, processes intents |
| `~Intent.kt` | Sealed class/interface of all user actions |
| `~UiState.kt` | Immutable data class representing everything the UI needs to render |
| `~Event.kt` | Sealed class of one-shot events (navigation, toast, etc.) |

**ViewModel rules**:
- Use `viewModelScope` for all coroutines. Never use `GlobalScope`.
- Expose `uiState` as `StateFlow` (via `MutableStateFlow` internally).
- Expose `eventFlow` as `Flow` (via `Channel` internally, converted with `receiveAsFlow()`).
- Accept all user actions through a single `sendIntent(intent: ~Intent)` function.
- When a `StateFlow`'s initial value differs from the async-loaded value for a first-frame-visible field, seed the initial value from a route argument to avoid a load flash — see **Navigation → Seeding initial UI state from the previous screen**.

**Screen rules**:
- The stateful overload (no parameters except navigation lambdas) obtains the ViewModel via `koinViewModel<~ViewModel>()`.
- It collects `eventFlow` inside `LaunchedEffect(Unit)` and dispatches to navigation lambdas or other side effects.
- It calls a stateless overload of `~Screen(uiState, sendIntent, ...)` to separate rendering from state management.

---

## Scaffold Requirement

**Every composable that represents a full page (named `~Screen`) MUST use `Scaffold`.**

- Apply `innerPadding` to the root content container. Failing to do so breaks edge-to-edge rendering.
- Correct pattern:

```kotlin
Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = { ... },
) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        // content
    }
}
```

- Do NOT skip `innerPadding` by using `Modifier.padding(0.dp)` or ignoring it entirely.
- Views (composables not representing a full page, named `~View`) do NOT need `Scaffold`.

---

## Color

**All colors must come from `com.kintmin.presentation.theme.Color.kt`. Hardcoded color values are forbidden.**

- Allowed: `seaBlue100`, `gray80`, `bloodyRed60`, `MaterialTheme.colorScheme.*`, etc.
- Forbidden: `Color(0xFFABCDEF)`, `Color.Red`, `Color(255, 0, 0)`

If a required color is missing from `Color.kt`, add it there first, then use it.

---

## Preview

**Every `@Composable` function must have a corresponding `@Preview`.**

- Wrap all previews in `JellyTubeTheme { ... }`.
- Place the preview function in the same file as the composable.
- **`androidx.compose.ui.tooling.preview.Preview` 를 쓴다.** CMP 가 그 패키지를 멀티플랫폼으로 제공한다(`org.jetbrains.compose.ui:ui-tooling-preview`). `org.jetbrains.compose.ui.tooling.preview.Preview`(components 쪽)는 CMP 1.12 에서 deprecated 됐다.
- **파라미터를 붙이지 않는다** — 멀티플랫폼 `@Preview` 는 `showBackground`/`backgroundColor` 를 받지 않는다. 배경이 필요하면 `JellyTubeTheme` 안에서 `Surface`/`Box` 로 직접 깐다.
- Provide realistic mock data. If a `UiState` is needed, add a `getMock()` companion factory.

```kotlin
@Preview
@Composable
fun FooViewPreview() {
    JellyTubeTheme {
        FooView(
            data = FooUiState.getMock(),
            sendIntent = {},
        )
    }
}
```

---

## Animation

- All animation composables and helpers belong in `com.kintmin.presentation.animation/`.
- Do NOT write complex animation code inline inside a screen or view composable.
- Extract the animation into a dedicated composable in `animation/` and call it from the view.
- Simple state-driven animations (e.g., `AnimatedVisibility`, `animateContentSize`) are acceptable inline.

---

## View Decomposition

When a composable grows complex, split it into `~View` composables in a dedicated sub-package.

**Reference**: `com.kintmin.presentation.ui.main.playlist.list_item`
- `PlaylistItemView.kt` — the list item composable
- `PlaylistItemAddView.kt` — the add-item variant

**Rules**:
- Sub-views go in a sub-package named after the logical grouping (e.g., `list_item/`, `header/`, `dialog/`).
- Each `~View.kt` file contains one primary composable and its preview.
- A view file should not exceed ~200 lines. If it does, decompose further.

---

## Navigation

**Every screen must have a corresponding `~Navigation.kt` file in a `navigation/` sub-package.**

Follow this exact structure (see existing files for reference):

```kotlin
// 1. Route — @Serializable data class or object
@Serializable
data class FooScreenRoute(val id: Int)

// 2. NavController extension to navigate TO this screen
fun NavController.navigateToFooScreen(
    id: Int,
    navOptions: NavOptions,
) = navigate(FooScreenRoute(id), navOptions)

// 3. NavGraphBuilder extension to REGISTER this screen
fun NavGraphBuilder.fooScreen(
    navigateToBack: () -> Unit,
    // ...other navigation callbacks
) {
    composable<FooScreenRoute> { backStackEntry ->
        FooScreen(
            navigateToBack = navigateToBack,
        )
    }
}
```

**Rules**:
- Routes are `@Serializable`. Use a `data class` when the route carries arguments, an `object` when it does not.
- The `NavController` extension is named `navigateTo~`.
- The `NavGraphBuilder` extension is named after the screen in camelCase (e.g., `fooScreen`, `playlistDetail`).
- Navigation callbacks passed into the Screen composable are plain lambdas (`() -> Unit`, `(Int) -> Unit`). Never pass `NavController` directly into a Screen.
- If the screen is part of a nested graph, wrap it in a `navigation<GraphRoute>` block with its own `@Serializable` graph route object (see `SettingScreenNavigation.kt` for reference).

### Seeding initial UI state from the previous screen (avoid load flash)

When a screen's `~UiState` field is filled from an **async source** (a `Flow`/DB/UseCase whose first emission arrives after the first frame) **and** that field is already known synchronously by the previous screen at the moment of navigation, **pass it as a route argument and use it as the initial value** of the `StateFlow` (`stateIn(..., initialValue)` or the `MutableStateFlow` seed). Otherwise the first frame renders the async default (e.g. `false`) and then "flips" when the real value arrives — a visible flash.

**Reference**: `PlaylistDetailScreenRoute.isBasePlaylist`. The playlist list already holds `PlaylistItemUiState.isBasePlaylist`, so it is passed through the route and used as the header's `stateIn` initial value; the base-playlist edit UI no longer flickers on entry.

**Rules**:
- The route arg is a **seed only**. The async flow stays the source of truth and overrides the seed on its first emission — so a wrong/absent seed is never a correctness bug, only a returned flash. Never gate logic on the seed instead of the loaded value.
- Add the seed as a **trailing parameter with a default** on both the route `data class` and the `navigateTo~` function, so callers that do not know the value (deep links, cross-feature entry points) keep compiling and simply fall back to the async default.
- Only seed values the previous screen **already has in hand**. Do NOT fetch extra data upstream, or widen a UseCase, just to produce a seed — that trades a flash for a worse coupling.
- This applies to any first-frame-visible field gated on async load (base/system flags, tab selection, header titles already shown in the list, etc.), not just `isBasePlaylist`.

### Adding a Deep Link destination

If a new screen must be reachable via a deep link:
1. Add the path constant to `DeepLinkConstants.Path` in `android:app`.
2. Add the URI pattern to `DeepLinkConstants.UriPattern` (private) and a builder to `DeepLinkConstants.UriBuilder`.
3. Add the parsing branch in `MainViewModel.sendNavigationIntents()` in `android:app`.
4. Add the corresponding `NavigationIntent` entry in `android:app`.
5. Wire the new intent in `MainNavHost` to call the appropriate `NavController.navigateTo~()` function.
6. Check if `AndroidManifest.xml` `intent-filter` needs updating.
