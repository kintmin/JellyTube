## Module: shared:domain

**Role**: Pure business logic — domain models, repository interfaces, and use cases. No platform dependencies.

---

## What belongs here

- **Domain models**: Immutable data classes representing core business concepts (e.g., `Playlist`, `AudioMedia`). Keep them free of serialization annotations.
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
