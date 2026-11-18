# JellyTube

## 언어 규칙
- 모든 응답·설명·커밋 메시지·문서는 한국어로 작성한다. 항상 존댓말로 응답한다.
- 코드·변수명·기술 용어는 영어를 쓰되 설명은 한국어로 한다.
- **예외: 루트 `AGENTS.md` 는 영어다** — 언어와 무관한 작업 지시라 도구가 그대로 읽는다.
  모듈 `AGENTS.md`(아키텍처 설명)는 한국어다.

## 작업 규율

루트 `AGENTS.md` 가 단일 진원지다. 여기에 요약하지 않는다.

@AGENTS.md

## 프로젝트 구조

Kotlin Multiplatform 앱. 하나의 공유 코어를 Android / iOS / Desktop 이 공유한다.

| 자리 | 담당 |
|---|---|
| `shared/domain` | 순수 비즈니스 로직 — 모델·Repository 인터페이스·UseCase |
| `shared/data` | 그 구현 — 로컬 DB·네트워크·파일·받는 서버 |
| `shared/presentation` | Compose Multiplatform UI 와 UI 상태(MVI). 두 플랫폼이 이것을 그대로 쓴다 |
| `shared/log` | 로깅 API 만. 구현은 앱 모듈 |
| `shared/file-share` | 파일 공유 프로토콜 상수·DTO. 의존성 그래프의 잎 |
| `android/app` | Android 진입점·조립과 **모든 OS 기능** |
| `ios/shared` + `ios/app` | iOS 진입점·조립(Kotlin)과 Xcode 앱·Swift 브릿지 |
| `desktop` | Compose Desktop. 파일 공유의 보내는 쪽 |

**의존성 방향**: `domain` 은 `log` 외에 아무것도 의존하지 않는다. 구현은 앱·조립 모듈이
Koin 으로 주입하고, 이 방향은 빌드 의존성으로 강제된다. 모듈별 상세는 각 `AGENTS.md` 다.

## 빌드

- 모듈 경로는 콜론: `:shared:data`.
- **Windows / PowerShell**: Gradle 은 `./gradlew.bat`(NOT `./gradlew`).
  iOS Kotlin 은 **컴파일까지 검증된다** — `:ios:shared:compileKotlinIosSimulatorArm64` 와
  `:ios:shared:compileTestKotlinIosSimulatorArm64`(실기기는 `…IosArm64`).
  **프레임워크 링크·`xcodebuild`·테스트 실행은 macOS 전용**이다 — Apple SDK 와 링커가
  필요하고 그 의존성은 Windows 에서 받을 수 없다.
- **macOS**: `./gradlew`. 프레임워크를 링크한 뒤 `xcodebuild` 로 앱을 빌드한다.
  **클론 직후에는 `scripts/setup-ios-vendor.sh` 를 한 번 돌려야 한다** — `ios/app/Vendor/` 가
  gitignore 대상이라 비어 있다.

## 스킬 / 에이전트 (`.claude/`)

- 스킬: `add-deep-link`, `room-entity-migration` — 여러 자리에 걸쳐 빠뜨리기 쉬운 작업만 둔다.
  한 모듈 안에서 끝나는 작업은 해당 `AGENTS.md` 와 기존 코드를 따른다.
- 에이전트: `error-handling-reviewer`, `security-reviewer`, `performance-reviewer`,
  `build-verifier`. 일반 리뷰는 내장 `/code-review`·`/review`·`/security-review`.
