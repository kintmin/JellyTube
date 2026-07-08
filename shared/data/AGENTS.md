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

## Local DB Conventions

- Database schema changes require a migration. Do not increment the version without providing a migration strategy.
- Entity models live in `local_db/model/`. They must NOT be exposed outside this module — map to domain models in `local_db/mapper/`.
- DAO interfaces live in `local_db/dao/`. Facades that compose multiple DAOs live in `local_db/dao_facade/`.
- **관찰 중 삭제될 수 있는 단일 행 `Flow` 쿼리는 반드시 nullable(`Flow<T?>`)로 선언한다.** Room은 관찰 중 테이블이 변경되면 재쿼리하는데, 반환형이 non-null 단일 객체(`Flow<T>`)면 결과가 0행일 때 `IllegalStateException("query result was empty...")`을 던진다. 관찰 도중 해당 행이 삭제될 수 있으면(예: 재생/상세 화면에서 트랙·플레이리스트 삭제) `Flow<T?>`로 선언하고 소비 측(RepositoryImpl)에서 `filterNotNull()`로 삭제 순간의 null emission을 흘려보낸다. 일회성 `suspend` 단일행 쿼리는 무효화 재실행이 없어 이 레이스와 무관하지만, 호출 시점에 행이 없을 수 있으면 nullable 또는 `runCatching` 방어가 필요하다.
