## Module: android:presentation

**Role**: Compose UI and UI state management only. No business logic. No OS-level code.

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
- Use `showBackground = true` for view-level composables.
- Provide realistic mock data. If a `UiState` is needed, add a `getMock()` companion factory.

```kotlin
@Preview(showBackground = true)
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
1. Add the path constant to `DeepLinkConstants.Path` in `android:platform`.
2. Add the URI pattern to `DeepLinkConstants.UriPattern` (private) and a builder to `DeepLinkConstants.UriBuilder`.
3. Add the parsing branch in `MainViewModel.sendNavigationIntents()` in `android:app`.
4. Add the corresponding `NavigationIntent` entry in `android:app`.
5. Wire the new intent in `MainNavHost` to call the appropriate `NavController.navigateTo~()` function.
6. Check if `AndroidManifest.xml` `intent-filter` needs updating.
