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
