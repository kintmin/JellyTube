## Module: shared:data

**Role**: KMP data layer — repository implementations, local DB, network, file access, and Python bridge data models.

---

## Source Set Priority Rule

**Prefer `commonMain`. Use `androidMain` / `iosMain` only when unavoidable.**

The decision tree for where to place code:

1. Can it be written in pure Kotlin without any platform SDK? → `commonMain`
2. Does it require a platform-specific driver or API (e.g., SQLite driver, file path resolver)? → `androidMain` / `iosMain`

**Forbidden**:
- Business logic split across `androidMain` and `iosMain`. If both platforms need the same logic, put it in `commonMain`.
- Duplicate implementations in `androidMain` and `iosMain` that differ only in trivial details. Find a common abstraction and put it in `commonMain`.

### 플랫폼 탈출구 — 네 갈래다

`commonMain` 으로 안 되는 것이 나오면 아래에서 고른다. `presentation` 의 탈출구와 달리 이 모듈은
**구현을 거의 전부 자기 안에 둔다** — 모듈 밖으로 뚫는 것은 빌드 플러그인이 강제할 때뿐이다.

| 방식 | 언제 | 현재 |
|---|---|---|
| **`interface` + Koin** (구현을 양쪽 소스셋에) | 상태를 들고 있거나 Repository 가 주입받아야 하는 것 | `audio_edit/AudioEditor`, `translation/LyricsTranslator`, `file_share` 의 Repository 2종 |
| **`expect`/`actual`** | 상태 없는 함수 하나로 끝나는 것 | `data_migration/isPlayableAudioExtension`, `network/createHttpClient`, `local_db/JellyTubeDatabaseConstructor`(Room 이 요구한다) |
| **소스셋별 독립 함수**(`commonMain` 에 선언이 없다) | 양쪽 시그니처가 **애초에 다른** 것 | `data_migration/localDeviceName` — Android 는 `(Context)` 를 받고 iOS 는 `suspend` 다(UIKit 은 메인 스레드에서만 읽는다). 묶을 공통 시그니처가 없으니 묶지 않는다 |
| **`interface` 만 두고 구현은 다른 모듈** | 빌드 플러그인이 애플리케이션 모듈을 요구할 때 | `python_bridge/PythonExecutor` · `LyricsTransliterator` — `chaquopy` 가 app 모듈 전용이라 **Android 구현만** `android:app` 에 있다. iOS 는 이 모듈 `iosMain` 의 브릿지다 — **비대칭이 정상이다** |

- **클래스는 `expect class` 로 가르지 않는다.** 공통 테스트에서 인스턴스를 만들 수 없어 그것을 쓰는
  Repository 가 통째로 테스트 불가가 된다. 계약은 `interface`, 바인딩은 Koin 이다 —
  `shared/presentation/AGENTS.md` 의 같은 규칙과 한 몸이다.
- **미지원 플랫폼도 바인딩을 비우지 않는다.** Repository 가 그 포트를 주입받으므로 비워 두면 그
  Repository 를 꺼내는 순간 Koin 이 터진다. `iosMain/audio_edit/AudioEditorImpl` 이 미지원 스텁인
  이유다. 진입점을 숨기는 일은 `presentation` 의 `isAudioEditSupported` 가 따로 한다 — **별개 일**이다.
- 한 패키지가 한 방식을 쓰는 것이 아니다. `data_migration/` 안에 expect/actual 과 소스셋별 독립
  함수와 순수 `commonMain` 코드가 함께 있다. **고르는 단위는 패키지가 아니라 선언 하나다.**

### 플랫폼 라이브러리는 `androidMain`/`iosMain` 의존성에만 적는다

`commonMain.dependencies` 에 넣으면 iOS 컴파일이 깨진다 — **그게 이 규율을 강제하는 장치다.**
문서가 아니라 컴파일러가 막는다. 그래서 라이브러리가 새어 나갈 자리는 adapter 한 파일로 좁혀진다.

| 라이브러리 | 소스셋 | 갇혀 있는 자리 |
|---|---|---|
| `media3-transformer` | `androidMain` | `audio_edit/AudioEditorImpl` |
| `mlkit-translate` | `androidMain` | `translation/MlKitLyricsTranslator` |

포트 시그니처에 그 라이브러리의 타입을 **하나도 올리지 않는다.** `AudioEditor` 는 경로 문자열과
domain 모델과 `Flow` 만 주고받으므로 `commonMain` 의 `AudioEditRepositoryImpl` 은 media3 의 존재를
모른다. adapter 는 `internal` 이라 모듈 밖으로도 나가지 않는다.

> **`media3` 가 두 모듈에 있는 것처럼 보이는 것에 대해**: 같은 그룹명을 쓰는 **다른 artifact** 다.
> 이 모듈은 `media3-transformer`(트랜스코더 — 파일 in, 파일 out)이고 `android:app` 은
> `media3-exoplayer` · `media3-session`(재생기 — 서비스·알림·수명주기)이다. 재생은 OS 수명주기를
> 타므로 `android:app` 에 두고 `PlaybackController` 포트로 뚫었고, 트랜스코딩은 파일 변환이라
> 여기다. **UI 에 닿지 않는 것이 이 모듈에 있을 근거다** — 애매한 자리가 아니다.

---

### 파일 (`local_file/`)

`FileManagerImpl` 은 플랫폼마다 있지만 같은 일은 `commonMain` 의 `FileManagerCore`(okio)가 한다. 경로 조립, 가사, 앱 로그, 삭제·목록이 여기다. 플랫폼은 디렉터리 자리(`AppDirectories`)와 플랫폼 API 가 필요한 일(메타데이터 추출, 이미지 압축, content URI, 공용 저장소 내보내기)만 갖는다.

## Repository Implementation Conventions

- One implementation per repository interface, named `~RepositoryImpl.kt`.
- Implement the interface defined in `shared:domain`.
- Inject DAO facades, network data sources, or DataStore — never inject `Context` directly if it can be avoided.
- Repository methods that stream data must return `Flow<T>`.
- Repository methods that perform mutations must be `suspend fun`.

### Repository scope: pure data logic only (no repository-of-repository)

- **A repository MUST NOT inject another repository.** Doing so is an overreach and a violation of these conventions.
- A repository owns only **pure data logic** over its own data sources (DAO/facade, network, file, DataStore): CRUD, plus domain logic that must be wrapped in a **single transaction** (e.g. `AudioMediaFacade.deleteOrphanAudioMedia()` — query + delete in one `immediateTransaction`).
- **Logic that is composed of multiple repositories — or that orchestrates several independent data-source operations together (read → transform → write across sources, conditional sequencing, cross-cutting concerns like logging) — belongs in a `shared:domain` UseCase, NOT in a repository.**
- Rule of thumb: if an implementation finds itself coordinating several repositories, sequencing independent operations, or reaching for a logging/event dependency, move that orchestration up to the use case layer. Keep each repository method a thin, single-responsibility data operation that the use case can compose.

---

## 파일 공유 서버 (`file_share/`)

PC 수신(Android·iOS)과 데이터 보내기(Android)가 쓰는 받는 서버는 **이 모듈에만 둔다.** Ktor 서버 의존성은 앱 모듈·presentation 에 두지 않는다. `shared:file-share` 는 Desktop 과 공유하는 프로토콜(상수·DTO)만 가진다.

| 자리 | 무엇 |
|---|---|
| `commonMain/file_share/FileShareServer` | 엔진(CIO) 한 벌. 바인드 뒤 취소·실패해도 포트를 놓고, 앞 서버가 포트를 놓은 뒤 다시 띄운다 |
| `commonMain/file_share/PcUploadRoutes` | `/upload`·`/upload/artist`·`/upload/thumbnail` 전송 처리 |
| `androidMain/file_share/` | `AndroidFileShareServerRepository`(PC 수신·데이터 보내기가 한 서버를 같이 쓴다), `FileShareServerModule`(이전 중 PC 경로 409), `migration/`(세션·경로), `NsdAdvertiser` |
| `iosMain/file_share/` | `PcFileShareServerRepositoryImpl`(Bonjour 광고·재등록, 복귀 때 서버 재기동), `PlayableOnlyPcUploadHandler`(재생 못 하는 형식은 받기 전에 거절) |

- **경로는 전송과 인가만 맡고 처리는 domain 의 처리기(`PcUploadHandler`·`MigrationSendHandler`)에 넘긴다.** data 가 UseCase 를 부르면 의존 방향이 거꾸로 된다. 처리기는 UseCase 가 만든다.
- 받는 도중 앱이 잠시 내려가도 마저 받는 일은 `ReceiveKeepAlive` 가 맡는다. iOS 구현은 `ios/shared` 가 바인딩하고(`continueInBackgroundWhile` 사용), Android 는 포그라운드 서비스가 붙잡아 그냥 실행한다.

### mDNS 광고

| 상태 | 광고 | 인스턴스 이름 |
|---|---|---|
| PC 수신만 켜짐 | `NSD_SERVICE_TYPE`(`_jellytube._tcp`) | `NSD_SERVICE_NAME` |
| 이전 세션이 열리고 페어링 전 (Android) | `MIGRATION_NSD_SERVICE_TYPE`(`_jellytube-mig._tcp`) + TXT `v`(프로토콜 버전) | 기기 이름(받는 기기가 이것으로 고른다) |
| 페어링 뒤(세션이 닫힐 때까지) | 없음 | — |

- 이전 세션이 열려 있는 동안은 PC 업로드 타입을 내리고 `/upload*` 를 409 로 거절한다. Desktop 이 이전 중인 기기에 업로드하지 않게 하는 두 겹이다. PC 가 데이터를 바꾸는 경로는 `FileShareServerModule` 의 `pcUploadRoutes { }` 안에 둔다 — 409 가 거기서 한 번에 걸린다.
- 본문을 받기 전에 거절할 때는 본문을 끝까지 읽어 버린다. 읽지 않으면 보내던 클라이언트가 응답 대신 연결 끊김을 받는다.
- 세션당 토큰은 코드를 맞힌 한 기기에만 준다. 이미 페어링한 세션에 맞는 코드로 다시 페어링하면 409, 틀린 코드면 401 이다(코드 없이 이전 중인지 알 수 없게). 토큰은 주소에 묶지 않는다(받는 기기의 IP 가 바뀌어도 이어 받는다).
- 등록에 실패한 광고는 몇 번 다시 등록한다. 실패를 무시하면 받는 기기가 찾지 못한다.

## Local DB Conventions

- Database schema changes require a migration. Do not increment the version without providing a migration strategy.
- Entity models live in `local_db/model/`. They must NOT be exposed outside this module — map to domain models in `local_db/mapper/`.
- DAO interfaces live in `local_db/dao/`. Facades that compose multiple DAOs live in `local_db/dao_facade/`.
- **관찰 중 삭제될 수 있는 단일 행 `Flow` 쿼리는 반드시 nullable(`Flow<T?>`)로 선언한다.** Room은 관찰 중 테이블이 변경되면 재쿼리하는데, 반환형이 non-null 단일 객체(`Flow<T>`)면 결과가 0행일 때 `IllegalStateException("query result was empty...")`을 던진다. 관찰 도중 해당 행이 삭제될 수 있으면(예: 재생/상세 화면에서 트랙·플레이리스트 삭제) `Flow<T?>`로 선언하고 소비 측(RepositoryImpl)에서 `filterNotNull()`로 삭제 순간의 null emission을 흘려보낸다. 일회성 `suspend` 단일행 쿼리는 무효화 재실행이 없어 이 레이스와 무관하지만, 호출 시점에 행이 없을 수 있으면 nullable 또는 `runCatching` 방어가 필요하다.
