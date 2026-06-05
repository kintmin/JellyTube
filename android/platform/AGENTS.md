## Module: android:platform

**Role**: All Android OS-level features. If it talks to the Android OS, it lives here.

## What belongs here

Any code that directly uses Android OS APIs for background execution, inter-process communication, or system events:

- `Service` subclasses (foreground or bound)
- `BroadcastReceiver` subclasses
- `Worker` / `CoroutineWorker` subclasses
- `NotificationChannel` creation, `NotificationCompat.Builder` usage
- `MediaSession`, `MediaController`, `MediaBrowser` bindings
- Deep link URI constants and URI builder helpers

---

## What does NOT belong here

- UI composables or ViewModels → `android:presentation`
- Log and Python bridge implementations → `android:app`
- Business logic or use cases → `shared:domain`
- Repository implementations → `shared:data`
- Navigation logic → `android:app` or `android:presentation`

> `android:platform` is strictly for OS-level concerns. It must not know about UI state or navigation.

---

## DeepLinkConstants

`DeepLinkConstants` (package `com.kintmin.platform.deeplink`) is the single source of truth for all deep link paths, query keys, and URI builders.

**Rules**:
- All path segment strings must be declared as `const val` inside `DeepLinkConstants.Path`.
- All query parameter key strings must be declared as `const val` inside `DeepLinkConstants.QueryKey`.
- URI pattern strings belong in the private `UriPattern` object.
- Public URI builders belong in `UriBuilder` object and must return `android.net.Uri`.
- When adding a new deep link destination:
  1. Add the path constant to `DeepLinkConstants.Path`.
  2. Add the URI pattern to `UriPattern`.
  3. Add a builder function to `UriBuilder`.
  4. Update `AndroidManifest.xml` `intent-filter` in `android:app` if the link must be interceptable from outside the app.
  5. Add the parsing branch in `MainViewModel.sendNavigationIntents()` in `android:app`.
- **Do not** hardcode URI strings outside of `DeepLinkConstants`.
- `DEEP_LINK_HOST` is `www.jellytube.com`. Changing it requires updating the manifest `intent-filter` as well.

---

## Services

- Every foreground service must call `startForeground()` with a valid notification before Android OS enforces it.
- Services that can be started/stopped externally must expose a `companion object` with `startService(context)` / `stopService(context)` static helpers (see `StepForegroundService` for reference).
- Do not start services from `android:presentation`. Route through events collected in a Screen composable or via `android:app`.

---

## WorkManager Workers

- Workers live under `worker/`. Worker-specific DI modules live under `worker/di/`.
- Workers must not hold references to UI components.
- Expose worker request builders or enqueue helpers from `worker/` — do not enqueue directly from ViewModel.

---

## BroadcastReceivers

- Receivers live under `receiver/`.
- Receivers must be lightweight: do the minimum work, then delegate to a Service or Worker for anything long-running.
- Register receivers that need runtime registration via the DI module or `android:app`.
