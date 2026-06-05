---
name: new-tca-feature
description: JellyTube의 ios/app 모듈에 새 TCA 기능을 규약대로 생성한다. Client/Feature/Item/ScreenView 4파일 + #Preview를 한 번에 스캐폴딩한다. KMP use case를 Swift async API로 브릿지. "iOS 화면 추가", "TCA feature 생성", "새 iOS 기능 만들어줘" 요청 시 사용. (new-mvi-screen의 iOS 대칭 스킬)
---

# New TCA Feature (iOS)

`ios/app` 모듈에 새 기능을 추가할 때 사용한다. 규약의 단일 진원지는 `ios/app/AGENTS.md`이며, 생성 전 해당 파일과 정본 참조(`Features/Playlist/`)를 읽어 스타일을 맞춘다.

## 입력 확인 (필요 시 1개만 질문)
- 기능 이름(PascalCase, 예: `PlaylistDetail`)
- 화면에 필요한 KMP use case (브릿지 대상)
- 화면이 표시할 필드 (→ `~Item`에 들어갈 것만)

## 생성 파일 (한 폴더에 정확히 이 구성)
`ios/app/JellyTubeIos/Features/<Name>/` 아래:

| 파일 | 역할 | 정본 |
|---|---|---|
| `<Name>Client.swift` | KMP use case를 Swift async API로 래핑 (`DependencyKey` 등록) | `PlaylistClient.swift` |
| `<Name>Feature.swift` | `@Reducer` — State/Action + reduce 로직 | `PlaylistFeature.swift` |
| `<Name>Item.swift` | 뷰 전용 데이터 모델 (KMP 도메인 모델 아님) | `PlaylistItem.swift` |
| `<Name>ScreenView.swift` | Feature의 Store에 바인딩된 SwiftUI 뷰 | `PlaylistScreenView.swift` |

## 핵심 규칙 (위반 금지)
1. **Client**: KMP `Flow`는 SKIE 생성 `AsyncSequence`로 소비하고 `AsyncThrowingStream`으로 감싸며 `continuation.onTermination`에서 취소. KMP 생성 타입을 `~Client` 밖으로 노출 금지 — 클로저 안에서 `~Item`으로 매핑. `shared` import는 `~Client`에서만.
2. **Feature**: `BindableAction` + `BindingReducer()` 포함(뷰의 `@Bindable` 작동용). 장기 구독(`~Client` Flow)은 `.cancellable(id:, cancelInFlight: true)`, 중복 구독은 플래그(`isSubscriptionActive`)로 가드. **Feature 안에서 네비게이션 로직 금지** — 네비는 부모/`NavigationStack` 바인딩이 담당.
3. **Item**: `Identifiable`, `Equatable`, `Sendable` 준수. `shared` import 금지. 비즈니스 로직 담은 computed property 금지 — 순수 데이터 백.
4. **ScreenView**: `@Bindable var store: StoreOf<~Feature>`. `WithViewStore` 사용 금지. 뷰 바디에 `Binding<T>` 직접 노출 금지 — `BindableAction` 경유 후 `$store.field`. `body`가 ~50줄 넘으면 private computed property / sub-`View`로 분해. 재사용 sub-view는 `Shared/UI/`.
5. **#Preview**: 모든 `View` struct에 `#Preview` — `EmptyReducer()`로 액션 미발화, 현실적 mock. 로딩/콘텐츠 상태가 갈리면 로딩용 `#Preview` 하나 더.
6. **인코딩**: 모든 파일 UTF-8(BOM 없음), 한글 주석/문자열 보존.

## 절차
1. `ios/app/AGENTS.md`와 `Features/Playlist/` 정본 4파일을 읽어 패턴 확인.
2. 위 4파일을 생성. Feature는 빈 State/Action부터 시작하되 컴파일되는 형태로.
3. `JellyTubeIosApp.swift`의 부모 Feature/`NavigationStack`에 새 Feature 연결 지점을 안내.
4. KMP use case가 아직 없으면 `new-repository-usecase` 스킬로 shared 쪽을 먼저 추가하고, iOS 프레임워크 재빌드 필요를 안내.

## 골격 예시
```swift
// <Name>Client.swift
struct FooClient: Sendable {
    var fetchAll: @Sendable () -> AsyncThrowingStream<[FooItem], Error>
}

extension FooClient: DependencyKey {
    static let liveValue = Self.kmp()
}
extension DependencyValues {
    var fooClient: FooClient {
        get { self[FooClient.self] }
        set { self[FooClient.self] = newValue }
    }
}
```
```swift
// <Name>Feature.swift
@Reducer
struct FooFeature {
    @ObservableState
    struct State: Equatable {
        var items: [FooItem] = []
        var isLoading = false
    }
    enum Action: BindableAction, Equatable {
        case binding(BindingAction<State>)
        case task
        case itemsUpdated([FooItem])
    }
    @Dependency(\.fooClient) var fooClient
    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in /* ... */ }
    }
}
```
