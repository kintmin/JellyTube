# JellyTube

## 언어 규칙
- 모든 응답·설명·커밋 메시지·문서는 한국어로 작성한다.
- 코드, 변수명, 기술 용어는 영어를 사용하되 설명은 한국어로 한다.
- 한국어 대화에서 항상 존댓말(높임말)로 응답한다.

## Claude 컨텍스트 진입점

이 파일은 **인덱스/포인터**다. 실제 규칙의 단일 진원지는 각 모듈의 `AGENTS.md`이며, 여기에 규칙을 복제하지 않는다. 파일을 건드리기 전에 해당 모듈의 `AGENTS.md`를 먼저 읽는다.

## 프로젝트 개요
Kotlin Multiplatform(KMP) 멀티플랫폼 앱. 하나의 공유 코어를 Android / iOS / Desktop이 공유한다.
- **공유 코어 `shared/`**: `domain`(순수 비즈니스 로직 — 모델/Repository 인터페이스/UseCase), `data`(구현 — RepositoryImpl/Room/네트워크/Python 브릿지), `log`(로깅 API), `file-share`(파일공유 상수/DTO)
- **Android `android/`**: `app`(진입점/DI/네비/딥링크), `platform`(OS 기능 — Service/Worker/Receiver/Notification/DeepLinkConstants), `presentation`(Compose UI, MVI)
- **iOS `ios/app/`**: 진입점 + TCA(Client/Feature/Item/ScreenView) + SKIE로 KMP 브릿지
- **Desktop `desktop/`**: Compose Desktop, 파일 전송 sender + mDNS

## 모듈 맵 — 건드리기 전 읽을 AGENTS.md
| 경로 prefix | 읽을 문서 |
|---|---|
| 루트 공통 규율 | `AGENTS.md` |
| `android/app/` | `android/app/AGENTS.md` |
| `android/platform/` | `android/platform/AGENTS.md` |
| `android/presentation/` | `android/presentation/AGENTS.md` |
| `shared/domain/` | `shared/domain/AGENTS.md` |
| `shared/data/` | `shared/data/AGENTS.md` |
| `shared/log/` | `shared/log/AGENTS.md` |
| `shared/file-share/` | `shared/file-share/AGENTS.md` |
| `ios/app/` | `ios/app/AGENTS.md` |
| `desktop/` | `desktop/AGENTS.md` |

여러 모듈에 걸친 작업(딥링크 추가, 새 화면, 새 데이터 흐름 등)은 **관련 AGENTS.md를 전부 읽고** 코드를 작성한다.

## 반드시 지킬 횡단 규칙 (상세는 루트 `AGENTS.md`)
- **파일 인코딩**: 항상 UTF-8(BOM 없음). 한글 주석/문자열 보존. 한글 포함 파일 작성 후 U+FFFD(치환 문자, regex `\x{FFFD}`) 깨짐을 grep으로 0건 확인. U+FFFD는 전송 중 손실이라 재저장으로 복구 안 됨 — 원문에서 다시 작성.
- **의존성 방향**: `shared:domain`은 아무것도 의존하지 않음. 구현체는 `android:app`(Composition Root)에서 Koin으로 주입. 빌드 의존성으로 강제됨.
- **단순성/외과적 변경**: 요청한 것만, 최소 코드로. 무관한 코드 리팩터 금지.

## 자주 쓰는 스킬 / 에이전트 (`.claude/`)
- 스킬: `new-mvi-screen`(Android 화면), `new-tca-feature`(iOS 기능), `add-deep-link`, `new-repository-usecase`, `room-entity-migration`
- 에이전트: `error-handling-reviewer`, `security-reviewer`, `performance-reviewer`, `build-verifier`
- 일반 리뷰는 내장 `/code-review`·`/review`·`/security-review` 사용.

## 빌드
- Windows / PowerShell. Gradle은 `./gradlew.bat`(NOT `./gradlew`). 모듈 경로는 콜론: `:shared:data` 등.
- iOS 빌드는 macOS + Xcode 필요(`ios/app/JellyTubeIos.xcodeproj`). Windows에서는 iOS 빌드 검증 불가.
