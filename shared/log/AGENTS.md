## Module: shared:log

**Role**: Ultra-lightweight KMP module that defines the logging API. Contains no implementation.

---

## This module is API-only

`shared:log` defines the `AppLog` interface/object and its associated models. It does NOT contain any implementation of logging (no Firebase, no Logcat, no file writing).

**Concrete implementations live in the app modules**:
- Android: `android:app` → `log_impl/AppLogImpl`
- iOS: `ios/shared` → `log/IosAppLog` (Swift 수집기는 `IosAppLogBridge` 로 받는다)
- Desktop: `desktop` → (Desktop log implementation)

Each app module registers its implementation with the DI container at startup.

---

## What does NOT belong here

- Any platform SDK import (`android.*`, `firebase.*`, `NSLog`, etc.) → goes in the app module implementation
- Actual log output (writing to Logcat, Firebase, file) → goes in the app module implementation
- DI wiring of the implementation → goes in the app module's DI module

---

## 두 구현이 같은 값을 보내야 하는 규칙

GA4 제약(이름 40자·값 100자·파라미터 25개·userId 256자, 값 없는 파라미터는 빼기)은 `model/AnalyticsEvent` 의 순수 함수 하나가 정한다. 플랫폼 구현은 이것을 그대로 보낸다.

## Size constraint

This module must stay small. If you find yourself adding non-trivial logic here, you are doing it wrong. The total line count of this module should remain in the low hundreds. If it grows beyond that, reassess.
