## Module: shared:domain

**Role**: Pure business logic — domain models, repository interfaces, and use cases. No platform dependencies.

---

## What belongs here

- **Domain models**: Immutable data classes representing core business concepts (e.g., `Playlist`, `AudioMedia`). Keep them free of serialization annotations. This **includes result/output types returned across a repository boundary** (e.g., `AddedAudioMedia`) — they are domain data and belong in `model/`, one class per file.
- **Repository interfaces**: `interface PlaylistRepository { fun fetchAll(): Flow<List<Playlist>> }`. Define what data you need; do not prescribe how it is stored.
- **Use cases**: Single-responsibility classes with an `operator fun invoke(...)` entry point.

```kotlin
class FetchAllPlaylistsUseCase(
    private val repository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> = repository.fetchAll()
}
```

---

## What does NOT belong here

- Repository implementations → `shared:data`
- Android SDK imports (`android.*`, `androidx.*`) → forbidden in this module
- iOS SDK imports → forbidden in this module
- Coroutine scope creation (`CoroutineScope`, `GlobalScope`) → use cases must not own scopes; callers control lifecycle
- DI wiring → DI modules for `shared:domain` live in `shared:data` or app modules

---

## Use Case Conventions

- One use case per file, named `~UseCase.kt`.
- Constructor-inject the repository interface(s) the use case depends on.
- `operator fun invoke(...)` is the single public entry point.
- Return `Flow<T>` for reactive streams, `suspend fun` for one-shot operations.
- Never catch exceptions inside a use case unless you are transforming the error type for a domain reason. Let callers handle failures.

---

## Repository Interface Conventions

- One interface per domain concept (e.g., `PlaylistRepository`, `AudioMediaRepository`).
- Methods that stream data return `Flow<T>`.
- Methods that perform a one-shot mutation are `suspend fun`.
- Repository interfaces must not reference any database, network, or file-system types. Use domain model types only.
- A repository file (`repository/~Repository.kt`) contains the **interface only**. Never declare a `data class` inline in it — if a method returns a new result/output type, put that type in its own file under `model/` and import it. (This is what separates `model/` from `repository/`: `model/` holds the *data shapes*, `repository/` holds the *contracts* that pass them.)

---

## Model / Placement Conventions

- `<feature>/model/` is the **single home for every domain `data class`** — core concepts (`AudioMedia`, `Playlist`) *and* result/output wrappers that cross a repository boundary (`AddedAudioMedia`). One class per file, named after the class.
- Do NOT co-locate a domain data class inside a `repository/` interface file. A result type returned by a repository method is a domain model → `model/`.
- A result type that is genuinely private to one use case *may* sit in that use case's file, but once it is returned by a repository interface it must move to `model/`.
- Data-layer DTOs (Room entities, facade return wrappers, network DTOs) do NOT belong here — they live in `shared:data`. Only platform-independent domain shapes go in `shared:domain/model/`.
