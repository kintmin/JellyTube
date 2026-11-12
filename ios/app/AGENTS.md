## Module: ios/ (app + shared)

**Role**: iOS 진입점과 조립. UI 는 `shared:presentation`(CMP)을 그대로 쓴다.

iOS 쪽은 두 곳으로 나뉜다. **둘 다 "조립과 OS 호출" 만 한다 — 화면도 비즈니스 로직도 없다.**

| 경로 | 언어 | 역할 |
|---|---|---|
| `ios/shared/` | Kotlin | iOS Composition Root. 프레임워크 선언 · Koin 조립 · Compose 진입점 · 도메인 워커 포트 구현 |
| `ios/app/` | Swift | Xcode 앱. 진입점(`JellyTubeIosApp`) · Kotlin 인터페이스의 Swift 구현(브릿지) · Info.plist |

> **TCA 는 더 이상 쓰지 않는다.** `~Client` / `~Feature` / `~ScreenView` / `IosFacades` /
> `~Facade.kt` 규약은 CMP 전환으로 전부 폐기됐고 해당 파일도 리포에 없다. 옛 문서를 보고
> 그 구조를 되살리지 않는다.

---

## 프레임워크는 하나다

`ios/shared` 만 framework 를 선언한다(`baseName = "shared"` — Swift 의 `import shared`).

**둘로 쪼개면 안 된다.** Kotlin/Native 프레임워크를 2개 만들면 런타임·Koin·코루틴 디스패처가
각각 따로 서서, 같은 타입이 서로 다른 클래스가 되고 Koin 그래프가 둘로 갈린다.

`export()` 는 **Swift 가 직접 이름을 부르는 모듈만** 한다 — 지금은 `domain`·`log`·`data`.
`presentation` 은 `api` 로 의존만 하고 export 하지 않는다. export 하면 화면과 ViewModel 전체가
Obj-C 헤더로 쏟아진다.

---

## 무엇을 어디에 두는가

### `ios/shared/` (Kotlin)

넷뿐이다. **그 외의 것을 여기 두지 않는다.**

1. `build.gradle.kts` — framework + SKIE
2. `IosKoin.kt` — `initIosKoin(...)`. `android:app` 의 `JellyTubeApplication` 과 짝이다.
   한쪽에 모듈을 더하면 다른 쪽도 본다
3. `MainViewController.kt` — Swift 에 노출되는 최상위 함수들
   (`startApp` / `mainViewController` / `handleDeepLink` / `handleNotificationTap` / `handleSharedAudioFile`)
4. 도메인 포트의 iOS 구현 — `domain` 에 플랫폼 구현을 넣지 않기 위해서다
5. Swift 가 구현할 Kotlin 인터페이스 — 프레임워크에 들어가는 모듈이라야 Swift 가 본다
   (`IosMediaControllerBridge`, `IosAppLogBridge`, `IosMigrationSourceBrowserBridge`)

**앱 레벨 ViewModel 은 파일 최상위에 `by lazy` 로 한 벌만** 둔다. iOS 는 단일 윈도우이고,
Compose 밖(`onOpenURL`)에서 들어온 이벤트를 화면과 **같은 인스턴스**로 넘겨야 한다.
`by lazy` 인 이유는 Koin 이 서기 전에 초기화되는 것을 막기 위해서다.

> `MainViewController.kt` 의 **파일 이름이 Obj-C 클래스 이름을 정한다**
> (`MainViewControllerKt.startApp()`). 파일명을 바꾸면 Swift 호출부가 깨진다.

### `shared/presentation/src/iosMain/` (Kotlin)

- `platform/*.ios.kt` — `expect`/`actual` 탈출구
- **`port/` 의 iOS 구현도 여기다.** Android 는 `Context` 가 필요해 `android:app` 에 두지만,
  iOS 구현은 그런 것이 없고 Kotlin/Native 가 UIKit·AVFoundation·CoreMotion 인터롭을 기본
  제공해서 인터페이스와 같은 모듈에서 구현된다. 바인딩은 `di/PresentationIosModule.kt`
- 예외는 `MediaControllerManager` 하나다 — 구현이 Swift 엔진에 기대서 `ios/shared` 가 가진다

### `ios/app/JellyTubeIos/` (Swift)

**Swift 가 맡는 것은 둘뿐이다.**

1. **Kotlin/Native 가 부를 수 없는 SDK** — Firebase(`Shared/Log/`), PythonKit(`PythonBridgeImpl/`)
2. **Kotlin 으로 쓰면 더 나빠지는 네이티브 엔진** — `AVAudioEngine` 재생(`Shared/MediaController/`),
   background `URLSession`(`PythonBridgeImpl/YoutubeDownloader.swift`)

---

## 브릿지 패턴 — Swift 가 Kotlin 인터페이스를 구현한다

네 군데가 같은 모양이다: `IosPythonExecutorBridge`(data), `IosMediaControllerBridge`(ios/shared),
`IosAppLogBridge`(ios/shared), `IosMigrationSourceBrowserBridge`(ios/shared — `NWBrowser` 탐색).

```kotlin
// Kotlin — 프레임워크에 들어가는 모듈에 선언한다
interface IosAppLogBridge {
    fun debug(tag: String, message: String)
}
```
```swift
// Swift — 구현하고 doInitIosKoin 에 넘긴다
final class AppLogBridgeImpl: IosAppLogBridge {
    func debug(tag: String, message: String) { ... }
}
```

**Rules**:
- 브릿지는 **원시 타입과 거울 DTO** 만 주고받는다. 포트가 쓰는 `presentation` 타입은
  Swift 가 볼 수 없어서다(export 하지 않는다). `IosMediaItem` 이 그래서 있는 거울 DTO 다
- **판정은 Kotlin 이 한다.** Swift 는 "시킨 일" 만 한다. 같은 판단을 두 플랫폼이 다르게 내리면
  그 자체가 버그다 — 딥링크 파싱, 로그 자르기 규칙, A-B 구간반복이 전부 Kotlin 에 있는 이유다
- 비동기는 `completionHandler` 로 받는다. SKIE 가 Swift `async` 로 바꿔 준다
- Swift 구현이 `@MainActor` 타입에 기대면 `@MainActor init` + `MainActor.assumeIsolated` 로
  **메인스레드 계약을 코드에 적는다**. nonisolated 로 두고 넘어가지 않는다

---

## Info.plist — `INFOPLIST_KEY_*` 를 믿지 않는다

**`GENERATE_INFOPLIST_FILE = YES` 라도 실제 `Info.plist` 파일을 둔다.**
일부 키는 `INFOPLIST_KEY_*` 빌드 설정에 넣어도 **생성된 plist 에 들어가지 않는다**
(확인된 것: `CADisableMinimumFrameDurationOnPhone`, `UIBackgroundModes`).
조용히 빠지므로 "설정했다" 고 믿고 넘어가면 기동 크래시나 백그라운드 중단으로 돌아온다.

지금 파일에 있는 것: `CADisableMinimumFrameDurationOnPhone`(CMP 필수) ·
`NSMotionUsageDescription`(걸음수) · `CFBundleURLTypes`(딥링크 스킴) ·
`CFBundleDocumentTypes`(공유 수신) · `UIBackgroundModes: audio`(백그라운드 재생) ·
`NSLocalNetworkUsageDescription` + `NSBonjourServices`(데이터 이전 탐색 — 둘 중 하나라도 없으면
탐색이 오류 없이 0건) · `NSAppTransportSecurity.NSAllowsLocalNetworking`(보내는 기기의 평문 HTTP).

---

## 진입 통로

| 들어오는 것 | 받는 자리 | 넘기는 곳 |
|---|---|---|
| 딥링크(`jellytube://`) | `onOpenURL` | `MainViewControllerKt.handleDeepLink` |
| 다른 앱의 "JellyTube 로 열기" | `onOpenURL` (`url.isFileURL`) | `MainViewControllerKt.handleSharedAudioFile` |
| 로컬 알림 탭 | `AppDelegate.userNotificationCenter(_:didReceive:)` | `MainViewControllerKt.handleNotificationTap` |
| 백그라운드 전송 완료 | `AppDelegate.handleEventsForBackgroundURLSession` | `YoutubeDownloader.shared` |

**Rules**:
- 주소 진입 통로는 `onOpenURL` **하나**다. `application(_:open:options:)` /
  `scene(_:openURLContexts:)` / `NSUserActivity` 를 쓰지 않는다
- 알림 탭은 `userInfo` 를 **그대로** Kotlin 에 넘긴다. 작업 결과 알림의 문구와 갈 화면은
  공통 `WorkResultNotice` 가 정하고 Android 알림도 같은 것을 쓴다.
  `userInfo` 를 쓰고 읽는 것은 둘 다 `IosLocalNotification.kt` 에 있다. Swift 는 키를 모른다
- Swift 가 하는 분기는 `url.isFileURL` 하나뿐이다. 그 뒤 판정은 전부 공통 코드가 한다
- 딥링크 경로·쿼리 키의 진원지는 `shared:presentation` 의 `DeepLinkConstants` 다.
  스킴을 바꾸면 `Info.plist` 의 `CFBundleURLSchemes` 도 함께 바꾼다

---

## Vendor/

`ios/app/Vendor/` 는 gitignore 대상이다. **클론 직후에는 비어 있어 빌드되지 않는다.**
`scripts/setup-ios-vendor.sh` 를 한 번 돌려 Python.xcframework 와 app-packages
(yt-dlp · pykakasi)를 받는다.

**yt-dlp 는 주기적으로 다시 받아야 한다.** Android 는 chaquopy 가 **빌드할 때마다**
`install("yt-dlp")` 를 다시 풀어 최신이 들어가지만, iOS 의 `Vendor/` 는 한 번 받아 두고
쓰는 스냅샷이라 가만두면 계속 묵는다. 묵은 yt-dlp 는 유튜브가 추출 경로를 바꾸면 서명이
모자란 주소를 돌려주고 전송이 **403 으로 거부**된다.

→ **다운로드가 간헐적으로 실패하면 코드를 보기 전에 이 스크립트부터 다시 돌린다.**

---

## iOS 에서 제공하지 않는 것

진입점을 숨기고, 포트는 **명시적 미지원 스텁**으로 바인딩한다
(`presentation/port/IosUnsupportedPorts.kt`, `data/audio_edit/AudioEditorImpl.kt`).
스텁이 없으면 그 화면에 들어가는 순간 Koin 이 터진다 — 바인딩을 비워 두는 선택지는 없다.

| 항목 | 이유 |
|---|---|
| 음원 편집 | Media3 Transformer + TarsosDSP 기반이라 전면 재작성이다 |
| PC 파일 수신 | 수신 서버(Ktor CIO)와 그 서버를 붙잡아 두는 포그라운드 서비스가 Android 에만 있다 |
| 드로잉 / 플로팅 UI | `MotionEvent` + S펜 전제. Apple Pencil 에 대응물이 없다 |
| 기기 저장(벨소리/알람/알림음) | 세 타깃 전부 30초 트리밍 + 포맷 변환이 선행 조건이라 음원 편집과 묶인다 |
| 번역(ML Kit) | 대응물이 iOS 18+ `Translation` 뿐인데 배포 타깃이 17.0 이다 |
| iosX64(인텔 맥 시뮬레이터) | CMP 가 1.11.0-alpha01 이후 배포하지 않는다 |

**Android 와 보장이 다른 것**: 사용자가 앱 스위처에서 강제 종료하면 background `URLSession`
전송도 OS 가 취소한다. 정책이라 우회 수단이 없다 — WorkManager 와 같은 보장은 iOS 에 없다.

---

## 빌드

```bash
./gradlew :ios:shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project ios/app/JellyTubeIos.xcodeproj -scheme JellyTubeIos \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

Xcode 빌드는 `:ios:shared:embedAndSignAppleFrameworkForXcode` 를 빌드 페이즈로 부른다 —
Kotlin 을 고쳤으면 Xcode 빌드만 돌려도 반영된다.
