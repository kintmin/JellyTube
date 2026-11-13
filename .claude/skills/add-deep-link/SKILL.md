---
name: add-deep-link
description: JellyTube에 새 딥링크 목적지를 규약대로 추가한다. 상수/빌더/파싱/NavigationIntent/MainNavHost는 shared:presentation(commonMain), Manifest intent-filter만 android:app. "딥링크 추가", "deep link 연결", "외부 링크로 화면 진입" 요청 시 사용.
---

# Add Deep Link Destination

딥링크는 **`shared:presentation` 의 `commonMain` 에서 끝난다.** 상수·주소 조립은
`com.kintmin.presentation.deeplink`, 파싱은 같은 패키지의 `DeepLinkParser`, 화면 이동 명령과
그래프는 `com.kintmin.presentation.navigation` 이다.

플랫폼에 남는 것은 **진입 통로와 OS 배선뿐**이다 — Android 는 Manifest `intent-filter` 와
`MainActivity.handleIntent(intent)`(Intent → 문자열), `PendingIntent` 가 `Uri` 를 요구하는
자리를 메우는 `com.kintmin.platform.deeplink.DeepLinkConstants` 어댑터. iOS 는 `onOpenURL`.

**판정은 공통이 하고 플랫폼은 실행만 한다.** 두 플랫폼이 같은 주소에 다른 답을 내면 그 자체가
버그다. 단일 진원지는 `shared/presentation/AGENTS.md` 와 `android/app/AGENTS.md` 이며,
작성 전 `DeepLinkConstants` 와 `DeepLinkParser.parse()` 의 기존 패턴을 확인한다.

## 입력 확인
- 진입할 대상 화면(이미 존재하는지 — 없으면 `new-mvi-screen` 먼저)
- path 세그먼트, query 파라미터 키
- 외부 인터셉트 필요 여부(필요 시 Manifest intent-filter)

## 5단계 절차 (순서·모듈 고정)
1. **`presentation/deeplink/DeepLinkConstants`** — `Path` 에 path 상수, `QueryKey` 에 query 키를
   `const val` 로 추가. 주소 조립이 필요하면 `UrlBuilder` 에 **문자열 반환** 함수를 더한다.
   값에 임의 문자가 들어가면 `UrlParts.encodeQueryValue` 를 거친다.
2. **`presentation/navigation/NavigationIntent`** — 새 목적지 엔트리 추가.
3. **`presentation/deeplink/DeepLinkParser`** — `parse()` 에 분기 추가.
   **포트를 주입받지 않는다** — 런타임 상태가 필요하면 파라미터로 받는다.
4. **`presentation/navigation/MainNavHost`** — 해당 `NavigationIntent` 수신 시
   `NavController.navigateTo~()` 호출하도록 `consumeNavigationIntent` 에 연결.
5. **플랫폼 진입 통로** — 외부에서 인터셉트해야 하면 `android:app` 의 `AndroidManifest.xml`
   `intent-filter` 갱신. `PendingIntent` 에서 열어야 하면 `android:app` 의
   `DeepLinkConstants.UriBuilder` 에 어댑터 한 줄(`UrlBuilder.~().toUri()`)을 더한다.

## 강제 규칙
- **주소 문자열을 `presentation` 의 `DeepLinkConstants` 밖에서 조립하지 않는다.** 안드로이드
  어댑터도 조립하지 않고 공통 결과를 `Uri` 로만 바꾼다.
- **`DeepLinkParser.parse()` 는 순수 함수다.** 우리 주소가 아니면 빈 목록을 돌려주고 호출자는
  아무것도 하지 않는다. 검증 실패를 사용자에게 알리지 않는다.
- 새 목적지로 가기 전 `NavigationIntent.PopAll` 이 먼저 나가야 한다(파서가 뿌리 경로 확인
  직후 넣는다).
- 분기 안에서 포기하면 **그때까지 쌓인 명령은 그대로 나간다.** 중간 포기 지점을 바꾸면
  동작이 바뀐다 — `DeepLinkParserTest` 가 분기별 순서를 고정한다.
- `navigationIntentFlow` 의 유일한 소비자는 `MainNavHost` — 다른 곳에서 collect 금지.
- 딥링크 처리 후 URI 소비: Android 는 `intent.data = null`.
- `DeepLinkConstants.HOST` 변경 시 Manifest `intent-filter` 도 함께 변경.
- 모든 파일 UTF-8(BOM 없음).

## 흐름 참고
- **Android**: `MainActivity.onNewIntent()/onCreate()` → `MainActivity.handleIntent(intent)` →
  `AppViewModel.handleDeepLink(uri.toString())` → `DeepLinkParser.parse(url, 재생상태)` →
  `navigationIntentChannel`(capacity=8) → `MainNavHost` 가 `navigationIntentFlow` 수집 →
  `NavController` 호출.
- **iOS**: `onOpenURL` → 조립 모듈(`:ios:shared`)의 최상위 `handleDeepLink(url)` → 같은 파서.

## 완료 확인
1. `./gradlew :shared:presentation:testAndroidHostTest` — 파서 테스트에 새 분기를 더했는지.
2. `./gradlew :android:app:assembleDebug` 와 `:ios:shared:linkDebugFrameworkIosSimulatorArm64`.
3. `shared/presentation/AGENTS.md` 의 commonMain 순수성 게이트가 0줄인지.
4. 5단계가 모두 반영됐는지 체크리스트로 확인(특히 2·3·4는 누락되기 쉽다).
