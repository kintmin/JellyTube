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
  "^import (android\.|java\.|javax\.|androidx\.(activity|core|palette|work|webkit|compose\.ui\.viewinterop)\.)|androidx\.compose\.ui\.platform\.Local(Context|View)|androidx\.lifecycle\.(lifecycleScope|repeatOnLifecycle)|String\.format|\bMath\.|System\.currentTimeMillis|org\.koin\.androidx"
```

결과가 **0줄**이어야 한다. `androidx.compose.*` / `androidx.navigation.*` / `androidx.lifecycle.compose.*` 는 CMP·JetBrains 포크가 같은 패키지명을 쓰므로 **허용**이다. 위 정규식은 Android 전용 하위 패키지와 심볼만 집어서 금지한다.

### 아직 commonMain 으로 올리지 못한 것 (승격 대기)

CMP 전환 2단계에서 전 소스를 `androidMain` 에 둔 상태다. 아래 순서로 `commonMain` 으로 옮긴다.

| 묶음 | 대상 | 선행 작업 |
|---|---|---|
| W1 | `theme/`, `extension/`, `util/`, `remember_state/`, `animation/`, 전체 `*Intent.kt`/`*Event.kt`/`*UiState.kt`, `di/`, `port/` | 없음 |
| W2 | 포트만 쓰는 ViewModel, `*Navigation.kt`, 아이콘/Coil 안 쓰는 `~View.kt` | 없음 |
| W3 | Coil(`LocalPlatformContext` + okio.Path), 다이얼로그/바텀시트(`PlatformFullScreenDialog`), lifecycle 폴링(`LifecycleResumeEffect`) | 탈출구 신설 |
| W4 | `SettingScreen`, `SettingDeviceSaveSelectScreen`, `SettingShareScreen`, `MainScreen`+`YoutubeWebView`, `PlayerDetailScreen`, `AudioEditScreen`, `LyricsEditScreen` | 탈출구 전부 |

**의도적으로 `androidMain` 에 남기는 것** — `expect` 를 만들면 iOS actual 이 영구히 죽은 코드가 되므로 추상화하지 않는다.
- `ui/custom_ui/image_drawing/ImageDrawingView.kt` — S펜 측면 버튼(`MotionEvent.BUTTON_STYLUS_PRIMARY`). CMP common 에 대응 API 가 없고 Apple Pencil 에는 그 버튼이 아예 없다
- `ui/custom_ui/floating_component/FloatingComponentEditor.kt` — `pointerInteropFilter` 기반 회전 제스처
- `ui/setting/app_log/AppLogScreen.kt` 의 `sendAppLogByEmail` — `filesDir` + FileProvider + `ACTION_SEND_MULTIPLE`
- `ui/setting/device_save/SettingDeviceSaveScreen.kt` 의 `startFirstAvailableActivity` — AlarmClock/Settings 액션 폴백 체인

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
