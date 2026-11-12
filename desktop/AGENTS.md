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

1. `NsdDiscovery` discovers Android devices via mDNS (`FileShareConstants.NSD_SERVICE_TYPE`). It takes the first device found — PC 수신 기기는 주변에 하나라는 전제다. 데이터 이전 중인 기기는 이 타입을 광고하지 않고, 업로드도 409 로 거절한다.
2. `FileUploader` sends files to the discovered device's HTTP endpoint. 업로드는 `X-File-Name`(확장자 필수)과 Content-Length 를 싣는다. 받는 쪽이 둘 다 검증한다.
3. Port numbers, service names, and endpoint paths come from `shared:file-share` (`FileShareConstants`). Do not hardcode them here.

---

## Runtime implementations

- **Log implementation**: Register a desktop-appropriate logger (e.g., `println`, a file logger) at app startup.
- These implementations are analogous to `android:app`'s `log_impl/` — they are runtime-bound, not OS-level features.
