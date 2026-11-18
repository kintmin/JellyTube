## Module: shared:log

**Role**: Ultra-lightweight KMP module that defines the logging API. Contains no implementation.

---

## This module is API-only

`shared:log` defines the `AppLog` interface/object and its associated models. It does NOT contain any implementation of logging (no Firebase, no Logcat, no file writing).

**Concrete implementations live in the app modules**:
- Android: `android:app` → `log_impl/AppLogImpl`
- iOS: `ios/shared` → `adapter/IosAppLog` (Swift 수집기는 `bridge/IosAppLogBridge` 로 받는다)

`desktop` 은 이 모듈을 의존하지 않는다 — 파일 전송 sender 라 수집기를 붙일 자리가 없다.

Each app module registers its implementation with the DI container at startup.

---

## What does NOT belong here

- Any platform SDK import (`android.*`, `firebase.*`, `NSLog`, etc.) → goes in the app module implementation
- Actual log output (writing to Logcat, Firebase, file) → goes in the app module implementation
- DI wiring of the implementation → goes in the app module's DI module

---

## 두 구현이 같은 값을 보내야 하는 규칙

GA4 제약(이름 40자·값 100자·파라미터 25개·userId 256자, 값 없는 파라미터는 빼기)은 `model/AnalyticsEvent` 의 순수 함수 하나가 정한다. 플랫폼 구현은 이것을 그대로 보낸다.

---

## 모델 이름에 `Firebase` 가 들어가는 것 — 인정된 예외

`model/FirebaseEvent` · `FirebaseLogName` · `FirebaseParamName` 은 수집기 이름을 그대로 쓴다.
`android/app/AGENTS.md` 의 **"port 이름에 플랫폼 기구를 넣지 않는다"**(`ExecuteYoutubeDownload`(WorkManager) ·
`MediaControllerManager`(media3) 를 걷어낸 그 규칙)와 어긋나 보이지만, **의도한 예외다.**

- **수집기가 비즈니스 결정과 엮여 있다.** 어떤 이벤트를 GA4 로 보내고 어떤 것을 크래시 리포터로
  보낼지는 플랫폼 사정이 아니라 제품 결정이다. 수집기별로 보낼 것이 갈리면 그때 이름이 갈리는
  것이 맞다 — `SentryEvent` 가 생겨도 `FirebaseEvent` 와 나란히 선다.
- **억지로 추상화하면 잃는다.** `AnalyticsEvent` 하나로 묶으면 수집기마다 다른 제약(이름 길이,
  파라미터 수, 예약어)을 어디에 둘지가 사라진다. 지금은 GA4 제약이 `AnalyticsEvent` 의 순수 함수
  하나에 모여 있고 그것이 테스트로 고정된다.
- `PlaybackController` 쪽 규칙은 **같은 일을 하는 기구가 플랫폼마다 다른** 경우였다(WorkManager ↔
  코루틴, media3 ↔ AVAudioEngine). 수집기는 그와 달리 **두 플랫폼이 같은 Firebase** 를 쓴다.

새 수집기를 더할 때 이 예외를 근거로 쓰지 않는다. 기구 이름이 드러나도 되는 것은 **수집기**뿐이고,
OS 기능은 여전히 `android/app/AGENTS.md` 의 규칙을 따른다.

## Size constraint

This module must stay small. If you find yourself adding non-trivial logic here, you are doing it wrong. The total line count of this module should remain in the low hundreds. If it grows beyond that, reassess.
