## Module: ios/app

**Role**: iOS app entry point, TCA architecture, KMP use case bridging, and runtime implementations.

---

## Architecture: TCA (The Composable Architecture)

Every feature follows this exact file structure. Use `Playlist` as the canonical reference.

| File | Role | Reference |
|---|---|---|
| `~Client.swift` | Wraps KMP use cases into a Swift-friendly async API | `PlaylistClient.swift` |
| `~Feature.swift` | `@Reducer` — owns State, Action, and the reduce logic | `PlaylistFeature.swift` |
| `~Item.swift` | View-specific data model (not the KMP domain model) | `PlaylistItem.swift` |
| `~ScreenView.swift` | SwiftUI view bound to the Feature's Store | `PlaylistScreenView.swift` |

---

## ~Client Rules

A `~Client` struct wraps KMP use case bridges so that the rest of the Swift code never imports `shared` directly except in the `~Client` extension.

**Structure**:

```swift
struct FooClient: Sendable {
    var fetchAll: @Sendable () -> AsyncThrowingStream<[FooItem], Error>
    var doSomething: @Sendable (_ param: String) async throws -> Void
}

// 1. Register as a TCA Dependency
extension FooClient: DependencyKey {
    static let liveValue = Self.kmp()
}

extension DependencyValues {
    var fooClient: FooClient {
        get { self[FooClient.self] }
        set { self[FooClient.self] = newValue }
    }
}

// 2. KMP bridge wiring
extension FooClient {
    static func kmp(
        fetchAllBridge: ... = ...,
        ...
    ) -> Self {
        Self(
            fetchAll: {
                AsyncThrowingStream { continuation in
                    let task = Task {
                        // SKIE-generated async sequence from KMP Flow
                        for try await items in fetchAllBridge.invoke() {
                            continuation.yield(items.map { FooItem(...) })
                        }
                        continuation.finish()
                    }
                    continuation.onTermination = { _ in task.cancel() }
                }
            },
            doSomething: { param in
                try await doSomethingBridge.invoke(param: param)
            }
        )
    }
}
```

**Rules**:
- KMP `Flow` return types must be consumed via SKIE's generated `AsyncSequence`. Wrap them in `AsyncThrowingStream` with proper cancellation via `continuation.onTermination`.
- Never expose KMP-generated types outside the `~Client` file. Map to Swift `~Item` types inside the closure.
- The `liveValue` must call `Self.kmp()`. Test values can provide mock closures.

---

## ~Feature Rules

A `~Feature` is a TCA `@Reducer`. It owns the canonical UI state and all business-level actions.

```swift
@Reducer
struct FooFeature {
    @ObservableState
    struct State: Equatable {
        var items: [FooItem] = []
        var isLoading = false
        @Presents var alert: AlertState<Action.Alert>?
    }

    enum Action: BindableAction, Equatable {
        case binding(BindingAction<State>)
        case task
        case itemsUpdated([FooItem])
        case operationFailed(String)
        case alert(PresentationAction<Alert>)
        enum Alert: Equatable {}
    }

    @Dependency(\.fooClient) var fooClient

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            // ...
        }
        .ifLet(\.$alert, action: \.alert)
    }
}
```

**Rules**:
- Always include `BindingAction` and `BindingReducer()` so that `@Bindable` works in the view.
- Long-running subscriptions (KMP Flow via `~Client`) must use `.cancellable(id:, cancelInFlight: true)`.
- Guard against duplicate subscriptions with a flag (e.g., `isSubscriptionActive`) before starting a `.run` effect.
- Do NOT perform navigation logic inside a Feature. Navigation is handled by parent features or `NavigationStack` bindings.

---

## ~Item Rules

A `~Item` is a Swift-side view model — a plain struct that holds exactly what the view needs.

```swift
struct FooItem: Identifiable, Equatable, Sendable {
    let id: Int
    let title: String
    // only fields the view actually uses
}
```

**Rules**:
- Must conform to `Identifiable`, `Equatable`, and `Sendable`.
- Must NOT import `shared`. Mapping from KMP domain models happens inside `~Client`.
- Do not add computed properties that contain business logic. Keep it a pure data bag.

---

## ~ScreenView Rules

The view is bound to `StoreOf<~Feature>` via `@Bindable`.

```swift
struct FooScreenView: View {
    @Bindable var store: StoreOf<FooFeature>

    var body: some View {
        // Use store.someState directly (thanks to @ObservableState)
        // Use $store.someBindableField for two-way bindings
        // Use store.send(.someAction) for user actions
    }
}
```

**Rules**:
- Use `@Bindable var store: StoreOf<~Feature>`. Do NOT use `WithViewStore`.
- Do NOT expose `Binding<T>` directly in the view body. If a sheet or text field needs a binding, handle it through `BindableAction` in the Feature, then use `$store.fieldName`.
- Break the view into private computed properties or private sub-`View` structs when `body` grows beyond ~50 lines.
- Sub-views that are reusable across features go in `Shared/UI/`.

---

## #Preview Requirement

**Every `View` struct must have a `#Preview` block.**

```swift
#Preview {
    FooScreenView(
        store: Store(
            initialState: FooFeature.State(
                items: [FooItem(id: 1, title: "Mock Item")]
            )
        ) {
            EmptyReducer()
        }
    )
}
```

- Use `EmptyReducer()` for previews so actions do not fire.
- Provide realistic mock data that exercises the non-empty state.
- If the view has a loading state and a content state, add a second `#Preview` for the loading state.

---

## Runtime Implementations

This module contains runtime-bound implementations that are not OS features:

- **Log implementation**: The concrete logging implementation (e.g., OSLog, Firebase) is provided here and registered with Koin at `JellyTubeIosApp` init.
- **Python bridge implementation** (`PythonBridgeImpl/PythonExecutorBridgeImpl.swift`): iOS-specific Python bridge. Not an OS feature — it is a runtime dependency.

These are registered during Koin initialization in `JellyTubeIosApp.swift` via `doInitIosKoin(...)`.
