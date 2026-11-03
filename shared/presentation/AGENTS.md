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
  "^import (android\.|java\.|javax\.|androidx\.(activity|core|palette|work|webkit|compose\.ui\.viewinterop)\.)|^import androidx\.compose\.ui\.platform\.Local(Context|View)|^import androidx\.lifecycle\.(lifecycleScope|repeatOnLifecycle)|^import org\.koin\.androidx|\bString\.format\(|[^.\w]Math\.|System\.currentTimeMillis\("
```

`^import` 로 묶은 항목은 import 줄만, 나머지는 호출 형태(`String.format(` / `System.currentTimeMillis(`)만 집는다. KDoc 에 API 이름을 적는 경우를 오탐하지 않기 위해서다.

결과가 **0줄**이어야 한다. `androidx.compose.*` / `androidx.navigation.*` / `androidx.lifecycle.compose.*` 는 CMP·JetBrains 포크가 같은 패키지명을 쓰므로 **허용**이다. 위 정규식은 Android 전용 하위 패키지와 심볼만 집어서 금지한다.

### 승격 현황

**178 / 212 파일이 `commonMain`** 이다(84%). `androidMain` 33, `iosMain` 1.

`androidMain` 에 남은 33개의 사유는 셋뿐이다.

| 사유 | 대상 | 해소 조건 |
|---|---|---|
| **플랫폼 탈출구 미구현** | `MainScreen`+`YoutubeWebView`(WebView·권한·ActivityResult), `SettingScreen`(커스텀 ActivityResultContract·권한), `AppLogScreen`(filesDir·FileProvider·ACTION_SEND_MULTIPLE), `SettingDeviceSaveScreen`(AlarmClock/Settings 폴백), `SettingDeviceSaveSelectScreen`(권한), `SettingShareScreen`(파일 피커), `PlayerDetailScreen`(Palette·BitmapFactory), `AudioEditScreen`·`LyricsEditScreen`·`PlaylistEditScreen` | 파일피커·권한·WebView·도미넌트색 탈출구 신설 |
| **공통화 불가(의도적)** | `ImageDrawingView`(S펜 `MotionEvent.BUTTON_STYLUS_PRIMARY`), `FloatingComponentEditor`·`FloatingComponentView`(`pointerInteropFilter`) | 없음. `expect` 를 만들면 iOS actual 이 죽은 코드가 된다 |
| **위 파일의 이웃** | 해당 `~Navigation.kt`(= `~ScreenRoute` 선언처)와 그 Route 를 `toRoute()` 로 읽는 ViewModel, 그리고 `di/PresentationModule.kt` | 짝이 되는 Screen 이 올라가면 함께 올라간다 |

`~Navigation.kt` 는 `@Serializable` Route 와 `NavGraphBuilder` 등록을 한 파일에 담는다(프로젝트 규약). 등록부가 Screen 을 참조하므로 **Screen 이 `androidMain` 이면 Route 도 `androidMain` 이고, 그 Route 를 읽는 ViewModel 도 따라간다.** 파일을 쪼개 Route 만 올리는 선택지가 있으나, 규약을 깨고 파일 수를 늘리는 대가가 Screen 승격 뒤 자동으로 해소되는 문제보다 크다고 판단해 하지 않았다.

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

`port/` 의 포트 9개(`MediaControllerManager`, `AudioPreviewPlayer`, `Execute*` 4종, `StepMeasurementController`, `FileShareServerController`)는 후자이며 `android:app` 의 `platform/di/PlatformModule.kt` 가 바인딩한다. 배치 규칙은 `android/app/AGENTS.md` 의 **Port implementations** 절이 단일 진원지다.

### 추상화 없이 치환만으로 끝낸 것

- Coil 13파일: `LocalContext.current` → `coil3.compose.LocalPlatformContext.current`, `File(path)` → `path.toPath()`(okio). 경로에 공백·`#` 이 있으면 깨지는 `"file://$path"` 문자열 방식은 쓰지 않았다.
- 폴링 3파일: `repeatOnLifecycle` + `lifecycleScope.launch` → `LifecycleResumeEffect` + `onPauseOrDispose { job.cancel() }`. **원래 코드는 RESUMED 를 벗어나도 루프가 취소되지 않는 버그였고 이 치환으로 함께 고쳐졌다.**
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
- **`org.jetbrains.compose.ui.tooling.preview.Preview` 를 쓴다.** CMP `@Preview` 는 **파라미터를 받지 않는다** — `showBackground`/`backgroundColor` 등을 붙이면 컴파일되지 않는다. 배경이 필요하면 `JellyTubeTheme` 안에서 `Surface`/`Box` 로 직접 깐다.
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
