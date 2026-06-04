## Module: android:app

**Role**: App entry point, navigation host, DI assembly, and runtime-bound implementations.

This module owns:
- `JellyTubeApplication` — Koin init, app-level setup
- `MainActivity` — edge-to-edge enablement, intent handling, lifecycle
- `MainNavHost` — top-level Compose navigation graph wiring
- `MainViewModel` — deep link parsing, shared file handling, navigation intent dispatch
- `NavigationIntent` — sealed class of all top-level navigation commands
- `di/AppModule` — Koin module that wires runtime implementations to their interfaces
- `log_impl/` — concrete `AppLog` implementation (Firebase, Logcat, etc.)
- `python_bridge_impl/` — concrete `PythonExecutor` implementation via Chaquopy

---

## What belongs here

### Runtime implementations

This module provides implementations that are **bound to the Android runtime** but are **not OS-level features**:

- **Log implementation** (`log_impl/AppLogImpl`): The `shared:log` module defines only the API. The concrete implementation (Firebase Analytics, Crashlytics, Logcat) lives here. Register it in `di/AppModule`.
- **Python bridge implementation** (`python_bridge_impl/PythonExecutorImpl`): Chaquopy is an Android build-time dependency; the implementation belongs here. Register it in `di/AppModule`.

> These are runtime dependencies, not OS features. Do NOT put them in `android:platform`.
> `android:platform` is for OS-level features (services, receivers, notifications).

---

## What does NOT belong here

- Foreground/background services → `android:platform`
- Push notification channels or builders → `android:platform`
- BroadcastReceivers → `android:platform`
- WorkManager workers → `android:platform`
- Deep link URI constants → `android:platform` (`DeepLinkConstants`)
- Business logic → `shared:domain`
- Repository implementations → `shared:data`
- UI composables → `android:presentation`

---

## Deep Link Handling

Deep links are handled by `MainViewModel` in coordination with `platform`'s `DeepLinkConstants`.

**Flow**:
1. `MainActivity.onNewIntent()` / `onCreate()` calls `MainViewModel.handleIntent(intent)`
2. `MainViewModel` reads `intent.data` (Uri) and delegates to `sendNavigationIntents(uri)`
3. `sendNavigationIntents` uses `DeepLinkConstants.Path` and `DeepLinkConstants.QueryKey` to parse segments
4. Navigation commands are sent as `NavigationIntent` via `navigationIntentChannel` (capacity=8 Channel)
5. `MainNavHost` collects `navigationIntentFlow` and calls the appropriate `NavController` functions

**Rules**:
- Adding a new deep link requires changes in BOTH `android:platform` (add path constant to `DeepLinkConstants`) AND `android:app` (add branch in `sendNavigationIntents`).
- Always consume the URI after processing: `intent.data = null`
- Always send `NavigationIntent.PopAll` before navigating to a new deep link destination.

---

## NavigationIntent Rules

- `NavigationIntent` is a sealed class in this module.
- Add new entries here when a new navigation destination is needed from `MainViewModel`.
- `MainNavHost` is the only consumer of `navigationIntentFlow`. Do not collect it elsewhere.
