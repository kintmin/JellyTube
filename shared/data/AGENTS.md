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
| `iosMain/file_share/` | `PcFileShareServerRepositoryImpl`(Bonjour 광고·재등록, 복귀 때 서버 재기동) |

- **경로는 전송과 인가만 맡고 처리는 domain 의 처리기(`PcUploadHandler`·`MigrationSendHandler`)에 넘긴다.** data 가 UseCase 를 부르면 의존 방향이 거꾸로 된다. 처리기는 UseCase 가 만든다.
- 받는 도중 앱이 잠시 내려가도 마저 받는 일은 `ReceiveKeepAlive` 가 맡는다. iOS 구현은 백그라운드 유예를 가진 presentation 이 바인딩하고, Android 는 포그라운드 서비스가 붙잡아 그냥 실행한다.

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
