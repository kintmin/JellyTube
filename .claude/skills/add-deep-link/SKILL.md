---
name: add-deep-link
description: JellyTube에 새 딥링크 목적지를 android:platform과 android:app에 걸쳐 규약대로 추가한다. DeepLinkConstants → MainViewModel 파싱 → NavigationIntent → MainNavHost → Manifest 순서를 강제. "딥링크 추가", "deep link 연결", "외부 링크로 화면 진입" 요청 시 사용.
---

# Add Deep Link Destination

딥링크는 `android:platform`(상수/URI)과 `android:app`(파싱/네비) 두 모듈에 걸친 절차다. 단일 진원지는 `android/platform/AGENTS.md`와 `android/app/AGENTS.md`이며, 작성 전 둘 다 읽고 `DeepLinkConstants`와 `MainViewModel.sendNavigationIntents()`의 기존 패턴을 확인한다.

## 입력 확인
- 진입할 대상 화면(이미 존재하는지 — 없으면 `new-mvi-screen` 먼저)
- path 세그먼트, query 파라미터 키
- 외부 인터셉트 필요 여부(필요 시 Manifest intent-filter)

## 6단계 절차 (순서·모듈 고정)
1. **`android:platform`** — `DeepLinkConstants.Path`에 path 상수를 `const val`로 추가. query 키는 `DeepLinkConstants.QueryKey`에 `const val`로 추가.
2. **`android:platform`** — private `UriPattern`에 URI 패턴 추가, `UriBuilder`에 `android.net.Uri` 반환 빌더 함수 추가. (`DEEP_LINK_HOST`는 `www.jellytube.com`)
3. **`android:app`** — `MainViewModel.sendNavigationIntents(uri)`에 파싱 분기 추가. `DeepLinkConstants.Path`/`QueryKey`로 세그먼트 파싱.
4. **`android:app`** — `NavigationIntent` sealed class에 새 목적지 엔트리 추가.
5. **`android:app`** — `MainNavHost`에서 해당 `NavigationIntent` 수신 시 적절한 `NavController.navigateTo~()` 호출하도록 연결.
6. **`android:app`** — 외부에서 인터셉트해야 하면 `AndroidManifest.xml` `intent-filter` 갱신.

## 강제 규칙
- URI 문자열을 `DeepLinkConstants` 밖에서 하드코딩 금지.
- 딥링크 처리 후 URI 소비: `intent.data = null`.
- 새 딥링크 목적지로 네비게이트하기 전 `NavigationIntent.PopAll` 먼저 전송.
- `navigationIntentFlow`의 유일한 소비자는 `MainNavHost` — 다른 곳에서 collect 금지.
- `DEEP_LINK_HOST` 변경 시 Manifest `intent-filter`도 함께 변경.
- 모든 파일 UTF-8(BOM 없음).

## 흐름 참고
`MainActivity.onNewIntent()/onCreate()` → `MainViewModel.handleIntent(intent)` → `sendNavigationIntents(uri)` → `navigationIntentChannel`(capacity=8) → `MainNavHost`가 `navigationIntentFlow` 수집 → `NavController` 호출.

## 완료 확인
1. `./gradlew.bat :android:platform:compileDebugKotlin` 및 `:android:app:compileDebugKotlin`.
2. 6단계가 모두 반영됐는지 체크리스트로 확인(특히 3·4·5는 누락되기 쉬움).
3. 모듈 경계가 의심되면 `android/platform/AGENTS.md`·`android/app/AGENTS.md`로 배치를 재확인한다.
