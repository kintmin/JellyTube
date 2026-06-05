## Module: desktop

**Role**: Compose Desktop app entry point, file upload client, mDNS discovery, and runtime implementations.

---

## What does NOT belong here

- File-sharing protocol constants and DTOs → `shared:file-share`
- Business logic → `shared:domain`
- Data persistence → `shared:data`

---

## File-sharing pattern

Desktop acts as the **sender** in the file-share flow.

1. `NsdDiscovery` discovers Android devices via mDNS (service type from `FileShareConstants`).
2. `FileUploader` sends files to the discovered device's HTTP endpoint.
3. Port numbers, service names, and endpoint paths come from `shared:file-share` (`FileShareConstants`). Do not hardcode them here.

---

## Runtime implementations

- **Log implementation**: Register a desktop-appropriate logger (e.g., `println`, a file logger) at app startup.
- These implementations are analogous to `android:app`'s `log_impl/` — they are runtime-bound, not OS-level features.
