## Module: shared:file-share

**Role**: Shared constants and DTOs for the file-sharing feature between `android:app`, `desktop`, and iOS.

---

## This module is data-only

`shared:file-share` contains only:
- `FileShareConstants.kt` — port numbers, service names, mDNS service types, or any shared string/int constants needed for the file-sharing protocol
- `FileShareDto.kt` — serializable data transfer objects used when sending or receiving file metadata over the network
- `MigrationDto.kt` — serializable DTOs of the device-to-device data migration protocol

---

## What does NOT belong here

- Business logic of any kind
- Network transport code (HTTP client, socket handling) → receiving server in `shared:data` (`file_share/`), sender in `desktop`
- File I/O → app modules
- UI → `shared:presentation` or `desktop`
- Platform-specific types (`android.*`, `java.io.*`) — keep it pure Kotlin so `desktop` can also depend on it

---

## Dependency constraint

`shared:data`(the receiving server and migration client), `desktop` and `ios/shared` depend on this module. Therefore:
- This module must have **zero platform-specific dependencies**.
- It is a KMP module with `jvm` + `iosArm64` + `iosSimulatorArm64` targets. Code lives only in `commonMain`.
  Android consumers get the `jvm` variant — there is no `android` target on purpose.
- Do not add transitive dependencies here. This module must remain a leaf node in the dependency graph.
