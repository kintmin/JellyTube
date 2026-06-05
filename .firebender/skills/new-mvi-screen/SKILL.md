---
name: new-mvi-screen
description: JellyTube의 android:presentation 모듈에 새 MVI 화면을 규약대로 생성한다. Screen/ViewModel/Intent/UiState/Event 5파일 + Navigation + @Preview를 한 번에 스캐폴딩한다. "새 화면 추가", "화면 만들어줘", "MVI 스크린 생성" 요청 시 사용.
version: 1.0.0
projectTypes: [android, kotlin]
autoTrigger: true
---

# New MVI Screen

`android:presentation` 모듈에 새 화면을 추가할 때 사용한다. 규약은 `android/presentation/AGENTS.md`가 단일 진원지이며, 생성 전 해당 파일과 기존 화면 1개(예: `playlist`)를 참고해 패키지/네이밍 스타일을 맞춘다.

## 입력 확인 (필요 시 1개만 질문)
- 화면 이름(PascalCase, 예: `PlaylistDetail`)
- 라우트 인자 유무 (있으면 `data class`, 없으면 `object`)
- 네비게이션 콜백 목록 (예: 뒤로가기, 다른 화면으로 이동)

## 생성 파일 (한 패키지에 정확히 이 구성)
`com.kintmin.presentation.ui.<group>.<feature>/` 아래:

| 파일 | 내용 |
|---|---|
| `<Name>Screen.kt` | stateful 래퍼(`koinViewModel`, `eventFlow` 수집) + stateless `<Name>Screen(uiState, sendIntent, ...)` + `@Preview` |
| `<Name>ViewModel.kt` | `uiState: StateFlow`(`MutableStateFlow` 백킹), `eventFlow: Flow`(`Channel`+`receiveAsFlow()`), `sendIntent(intent)` |
| `<Name>Intent.kt` | 모든 사용자 액션 sealed interface |
| `<Name>UiState.kt` | 불변 data class + `companion object { fun getMock() }` |
| `<Name>Event.kt` | 1회성 이벤트(네비/토스트) sealed class |
| `navigation/<Name>Navigation.kt` | `@Serializable` route + `NavController.navigateTo<Name>` + `NavGraphBuilder.<name>` |

## 핵심 규칙 (위반 금지)
1. **MVI**: ViewModel은 `viewModelScope`만 사용, `GlobalScope` 금지. `Context`/`View` 참조 금지. 액션은 `sendIntent` 단일 진입.
2. **Scaffold**: full-page `~Screen`은 반드시 `Scaffold` 사용 + 루트 컨테이너에 `innerPadding` 적용. `Modifier.padding(0.dp)`로 회피 금지.
3. **Color**: 색상은 `com.kintmin.presentation.theme.Color.kt` 또는 `MaterialTheme.colorScheme.*`에서만. 하드코딩 `Color(0xFF...)` 금지. 없으면 `Color.kt`에 먼저 추가.
4. **Preview**: 모든 `@Composable`에 `@Preview` 추가, `JellyTubeTheme { }`로 감싸고 `UiState.getMock()` 사용.
5. **Navigation**: route는 `@Serializable`. 화면에는 `NavController`가 아니라 plain 람다(`() -> Unit` 등)만 전달.
6. **View 분해**: 복잡해지면 `~View.kt`로 분리하고 하위 패키지(`list_item/`, `header/`)에 배치. 한 파일 ~200줄 초과 금지.
7. **인코딩**: 모든 파일 UTF-8(BOM 없음), 한글 주석/문자열 보존.

## 절차
1. `android/presentation/AGENTS.md`와 기존 참조 화면을 읽어 스타일 확인.
2. 위 6+1 파일을 생성. ViewModel은 빈 상태부터 시작하되 컴파일되는 형태로.
3. NavGraph에 등록이 필요하면 `MainNavHost` 연결 지점을 안내(딥링크 진입이면 `add-deep-link` 스킬과 연계).
4. 생성 후 `./gradlew.bat :android:presentation:compileDebugKotlin` 권장, `@Preview` 렌더 확인.

## 골격 예시
```kotlin
// <Name>ViewModel.kt
class FooViewModel(/* useCases */) : ViewModel() {
    private val _uiState = MutableStateFlow(FooUiState())
    val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

    private val _eventFlow = Channel<FooEvent>(Channel.BUFFERED)
    val eventFlow: Flow<FooEvent> = _eventFlow.receiveAsFlow()

    fun sendIntent(intent: FooIntent) {
        when (intent) {
            /* ... */
        }
    }
}
```
```kotlin
// <Name>Screen.kt (stateful)
@Composable
fun FooScreen(navigateToBack: () -> Unit) {
    val viewModel = koinViewModel<FooViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) { /* navigate / toast */ }
        }
    }
    FooScreen(uiState = uiState, sendIntent = viewModel::sendIntent)
}
```
