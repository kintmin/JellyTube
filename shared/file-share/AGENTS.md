## Module: shared:file-share

**Role**: Shared constants and DTOs for the file-sharing feature between `android:app` and `desktop`.

---

## This module is data-only

`shared:file-share` contains only:
- `FileShareConstants.kt` — port numbers, service names, mDNS service types, or any shared string/int constants needed for the file-sharing protocol
- `FileShareDto.kt` — serializable data transfer objects used when sending or receiving file metadata over the network

---

## What does NOT belong here

- Business logic of any kind
- Network transport code (HTTP client, socket handling) → `android:platform` or `desktop`
- File I/O → app modules
- UI → `android:presentation` or `desktop`
- Platform-specific types (`android.*`, `java.io.*`) — keep it pure Kotlin so `desktop` can also depend on it

---

## Dependency constraint

Both `android:app` and `desktop` depend on this module. Therefore:
- This module must have **zero platform-specific dependencies**.
- Keep it as pure Kotlin (no KMP source set split needed unless a future need arises).
- Do not add transitive dependencies here. This module must remain a leaf node in the dependency graph.
