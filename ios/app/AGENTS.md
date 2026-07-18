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

## KMP Facade (shared:data / iosMain)

**Do NOT create one bridge class + `create...` factory per UseCase.** That multiplies boilerplate by 80. Instead, expose **one Facade per feature** that groups the feature's UseCases via **constructor injection**, and resolve it through the single `IosFacades` object.

**기본은 domain 모델을 그대로 노출한다. DTO를 만들지 않는다.** 원시 필드(String/Int/Bool/경로문자열)는 Swift `~Client`의 `init(from:)`에서 직접 복사하면 된다. DTO는 필드를 중복 정의하는 오버헤드라 기본값이 아니다.

**예외는 Swift가 열 수 없는 리치 타입뿐이다.** `kotlin.time.Duration`은 value class라 Kotlin/Native가 불투명하게 내보내 Swift에서 `.inWholeSeconds`를 못 부른다(`LocalDateTime`도 유사). 이런 필드**만** Facade에 **변환 헬퍼 메서드**를 두어 원시 타입으로 준다. `Flow`(SKIE 처리)와 마찬가지로, "Swift에서 다룰 수 없는 것만 Kotlin 경계 헬퍼로 흡수"하는 원칙이다. **domain 모델은 절대 원시화하지 않는다.**

Reference: `PlaylistFacade.kt`(모델 반환 + `playTimeSeconds` 헬퍼), `IosFacades.kt`.

```kotlin
class FooFacade(
    private val fetchAllFlow: FetchAllFooFlowUseCase,
    private val doSomething: DoSomethingUseCase,
) {
    fun observeAll(): Flow<List<Foo>> = fetchAllFlow()               // domain 모델 그대로 (Flow는 SKIE 처리)
    suspend fun doIt(param: String) = doSomething(param).getOrThrow()  // Result → throws
    fun durationSeconds(foo: Foo): Long = foo.playTime.inWholeSeconds  // Swift가 못 여는 필드만 변환
}

object IosFacades : KoinComponent {                                 // Swift는 IosFacades.shared 로 접근
    fun foo(): FooFacade = get()
}

val iosFacadeModule = module { factory { FooFacade(get(), get()) } }  // initIosKoin의 modules()에 추가
```

**Rules**:
- **기본: DTO 없음.** Facade는 domain 모델을 반환하고 Swift `~Client`가 `init(from:)`에서 원시 필드를 직접 복사한다.
- **리치 타입(`Duration`/`LocalDateTime`)만** Facade 변환 헬퍼(`durationSeconds(foo:)`처럼 도메인 모델을 받아 원시 반환)로 처리한다. 이 헬퍼는 반드시 Kotlin에 둔다(Swift가 해당 타입을 못 열기 때문).
- `Flow`는 그대로 `Flow<List<Model>>`로 노출한다. SKIE가 경계에서 `AsyncSequence`로 변환한다.
- Use **named methods** (`observeAll`, `doIt`), never `operator fun invoke()` — `.invoke()`는 Swift 셀렉터가 지저분하다.
- Facade는 생성자 주입만 사용한다. `get<>()` 서비스 로케이터를 흩뿌리지 않는다. (`IosFacades`가 유일한 예외 창구)
- 새 기능 추가 시 Facade 클래스 1개 + `iosFacadeModule`에 factory 한 줄 + `IosFacades`에 리졸버 한 줄.
- **DTO는 예외적으로만** 만든다 — 리치 필드가 여럿이라 헬퍼가 지저분해지거나, `~Client`가 domain 타입을 아예 import하지 않게 강제하고 싶을 때.

---

## ~Client Rules

A `~Client` struct wraps a **single feature Facade** into a Swift-friendly async API. The rest of the Swift code never imports `shared` directly except in the `~Client` extension.

**Structure**:

```swift
struct FooClient: Sendable {
    var fetchAll: @Sendable () -> AsyncThrowingStream<[FooItem], Error>
    var doSomething: @Sendable (_ param: String) async throws -> Void
}

// 1. Register as a TCA Dependency
extension FooClient: DependencyKey {
    static let liveValue = Self.live()
}

extension DependencyValues {
    var fooClient: FooClient {
        get { self[FooClient.self] }
        set { self[FooClient.self] = newValue }
    }
}

// 2. Facade wiring — Facade 하나만 잡는다
extension FooClient {
    static func live(facade: FooFacade = IosFacades.shared.foo()) -> Self {
        Self(
            fetchAll: {
                KmpStream.of(facade.observeAll) { foos in foos.map { FooItem(from: $0, facade: facade) } }
            },
            doSomething: { param in try await facade.doIt(param: param) }
        )
    }
}

// 원시 필드는 self.init에서 직접 복사. Swift가 못 여는 리치 필드만 Facade 헬퍼로 받는다.
private extension FooItem {
    init(from foo: Foo, facade: FooFacade) {
        self.init(/* id: Int(foo.id), name: foo.name, ..., seconds: Int(facade.durationSeconds(foo: foo)) */)
    }
}
```

**Rules**:
- KMP `Flow` 소비는 손으로 `AsyncThrowingStream`을 짜지 말고 **`KmpStream.of(_:map:)`** 헬퍼(`Shared/Kmp/KmpStream.swift`)를 쓴다. 취소 처리(`onTermination`)가 헬퍼에 들어 있다.
- domain 모델 → `~Item` 매핑은 `private extension ~Item { init(from:facade:) }`로 Client 파일에 격리한다. 원시 필드는 직접 복사, 리치 필드만 `facade` 헬퍼 호출. `~Item` 정의 파일 자체는 `shared`를 import하지 않는다.
- The `liveValue` must call `Self.live()`. Test values can provide mock closures.
- Client는 Facade **하나만** 주입받는다. UseCase 브릿지를 여러 개 주입받는 형태(구식)는 금지.

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

## Modal Presentation Rules (정석 — 통일 필수)

**All modal presentation (alert / sheet / popover / fullScreenCover) MUST use tree-based `@Presents` + `.ifLet`.** Do NOT mix styles — a loose `Bool` flag (`isXxxPresented`) plus scattered state fields for one modal and `@Presents` for another is forbidden.

**Why**: `@Presents`는 "표시 여부 + 그 모달이 소유한 상태"를 한 옵셔널로 묶어 상태의 진실을 하나로 만든다. `Bool` + 별도 필드 방식은 표시 여부와 데이터가 따로 놀아 불일치가 생기고, TCA의 자식 리듀서/`onDismiss`/취소 연동을 못 받는다.

**Pattern** — 입력값을 가진 시트(예: 새 플레이리스트 추가)는 자식 State를 옵셔널로 소유한다:

```swift
@ObservableState
struct State: Equatable {
    @Presents var alert: AlertState<Action.Alert>?
    @Presents var addPlaylist: AddPlaylistFeature.State?   // 시트도 @Presents
}

enum Action {
    case alert(PresentationAction<Alert>)
    case addPlaylist(PresentationAction<AddPlaylistFeature.Action>)
    case addButtonTapped
}

// 열기: 자식 State를 세팅하면 시트가 뜬다
case .addButtonTapped:
    state.addPlaylist = AddPlaylistFeature.State()
    return .none

var body: some ReducerOf<Self> {
    BindingReducer()
    Reduce { state, action in /* ... */ }
        .ifLet(\.$alert, action: \.alert)
        .ifLet(\.$addPlaylist, action: \.addPlaylist)   // 시트도 .ifLet
}
```

- 아주 사소한 시트라도 `Bool` 대신 `@Presents var xxx: EmptyState?` 또는 전용 자식 Feature로 모델링한다.
- 뷰에서는 `.sheet(item: $store.scope(state: \.addPlaylist, action: \.addPlaylist))` 형태로 바인딩한다. `.sheet(isPresented:)` + 별도 바인딩 금지.

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
- Do NOT expose `Binding<T>` directly in the view body. A text field binding goes through `BindableAction` → `$store.fieldName`. A sheet/alert binds via `$store.scope(state:action:)` against its `@Presents` field (see Modal Presentation Rules) — never `.sheet(isPresented:)`.
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
