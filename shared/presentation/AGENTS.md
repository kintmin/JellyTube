## Module: shared:presentation

**Role**: Compose Multiplatform UI 와 UI 상태 관리만. 비즈니스 로직 없음. OS 코드 없음.

---

## 소스셋 규약

| 소스셋 | 무엇을 두는가 |
|---|---|
| `commonMain` | **기본 자리.** 화면·ViewModel·Intent·UiState·Event·Navigation·theme·extension·port |
| `androidMain` | `expect` 선언의 Android `actual`, 그리고 공통화가 불가능해 의도적으로 남긴 코드 |
| `androidHostTest` | 단위테스트. **`commonTest` 를 만들지 않는다** — mockk 가 JVM 전용이라 iOS 컴파일이 깨진다 |
| `iosTest` | iOS 전용 코드의 단위테스트 |
| `iosMain` | iOS `actual` 만. 포트의 iOS 구현은 iOS 조립 모듈이다 |

### 순수성 게이트

macOS 에서는 iOS 컴파일이 돌아 위반을 잡아 주지만, **Windows 에서 작업하면 `commonMain` 에
플랫폼 전용 API 를 넣어도 컴파일러가 잡아주지 않는다.** 아래를 그 자리의 통과 기준으로 쓴다.

```powershell
rg -n --glob "shared/presentation/src/commonMain/**/*.kt" `
  "^import (android\.|java\.|javax\.|androidx\.(activity|core|palette|work|webkit|compose\.ui\.viewinterop)\.)|^import androidx\.compose\.ui\.platform\.Local(Context|View|Configuration)|^import androidx\.lifecycle\.(lifecycleScope|repeatOnLifecycle)|^import org\.koin\.androidx|\.format\(|[^.\w]Math\.|System\.currentTimeMillis\("
```

결과가 **0줄**이어야 한다. `androidx.compose.*` · `androidx.navigation.*` ·
`androidx.lifecycle.compose.*` 는 CMP 포크가 같은 패키지명을 쓰므로 **허용**이다. 위 정규식은
플랫폼 전용 하위 패키지와 심볼만 집어서 금지한다.

`^import` 로 묶은 항목은 import 줄만, 나머지는 호출 형태만 집는다. KDoc 에 API 이름을 적는 경우를
오탐하지 않기 위해서다 — 그러니 **주석에도 호출 형태를 적지 않는다.**

---

## 플랫폼 탈출구

### 만들기 전에 — 사용처가 하나면 만들지 않는다

**탈출구는 두 번째 호출자가 생길 때 만든다**(루트 `AGENTS.md` §2). `expect` 를 하나 만들면
양쪽 소스셋에 파일이 생기고, 한쪽이 그 기능을 제공하지 않을 거면 그 `actual` 은 **영구히 죽은
코드**로 남는다. 선언이 공통에 있어 나중에 지우기도 어렵다.

쓰는 자리가 하나면 그 자리에 두고, 같은 것이 또 필요해질 때 올린다. 올리기 전에 **그 폴더를
열어 이미 있는지 본다**(루트 `AGENTS.md` §7).

### 분기 기준은 "구현이 어느 모듈에 있나" 하나다

| 구현 위치 | 방식 | 왜 |
|---|---|---|
| 이 모듈의 플랫폼 소스셋 | **`expect`/`actual`** (`platform/`) | 컴파일 타임에 actual 누락을 잡는다. DI 는 런타임에 터진다 |
| **다른 모듈**(앱·조립 모듈) | **`interface` + Koin** (`port/`) | `expect`/`actual` 은 한 모듈 안의 소스셋 분기라 다른 모듈의 구현에 닿을 수 없다 |

- Compose 로컬이 필요하면 `@Composable expect fun remember~()` 로 **생성만** `expect` 한다.
- **`expect class` 는 쓰지 않는다.** 공통 테스트에서 인스턴스를 만들 수 없어 그 타입을 쓰는
  로직이 통째로 테스트 불가가 된다. 계약은 `interface`, 생성만 `expect`.
- **커스텀 `CompositionLocal` 은 만들지 않는다.** 위 두 가지로 충분하고, 세 번째 경로가 생기면
  "이건 어느 쪽이냐" 를 매번 따져야 한다.
- **플랫폼 뷰 객체를 시그니처에 노출하지 않는다.** 노출하면 그 타입이 호출자까지 전염된다.
  핸들이 생명주기와 생성 실패 상태까지 소유하고, 호출자는 그 핸들만 본다.
- **기능을 제공하지 않는 플랫폼은 진입점을 숨긴다.** `expect val` 상수로 가린다 — 함수가 아니다.
  포트를 미지원 스텁으로 막는 것과 **별개 일**이다(막아도 진입점이 보이면 눌린다).
  **기능 플래그가 아니다** — 켜고 끄는 설정은 저장소에 둔다.
- 플랫폼 API 를 타는 자리라도 **순수한 계산을 떼어내면 `iosTest` 에서 돌릴 수 있다.** 주석으로
  규칙을 적기 전에 이쪽을 본다 — 루트 `AGENTS.md` §6.

---

## 포트 (`port/`)

OS 런타임 기능은 이 모듈이 **인터페이스만** 선언하고 구현은 앱·조립 모듈에 둔다. 배치 규칙은
`android/app/AGENTS.md` 의 **port 와 adapter** 절이 단일 진원지다.

- **인자 전달로 해결되면 포트를 만들지 않는다.** 값 하나를 넘기면 되는 경우는 화면·그래프
  파라미터로 받는다.
- 순수 판정 로직은 포트가 아니다. `shared:domain` 에 둔다.

### `port/model/` — 포트 경계를 오가는 데이터

포트 자체는 인터페이스지만, 그 경계를 넘는 데이터와 **양쪽 adapter 가 같은 말을 써야 하는
사용자향 문구**는 여기 한 벌만 둔다.

- **포트 시그니처에 실리지 않아도 여기다.** adapter 가 내부에서만 쓰는 것도 방향이 같다 —
  **이 모듈이 선언하고 플랫폼이 소비한다.** 딥링크를 조립하는 것은 `shared:domain` 으로
  내릴 수도 없다(domain 은 상위 레이어를 의존하지 않는다).
- **화면이 쓰는 데이터는 여기가 아니다.** 그쪽은 `~UiState` 다. 판정 기준은 소비처다 — 화면이
  읽으면 `UiState`, adapter 가 읽으면 `port/model/`.

### 포트를 쓰는 공통 규칙 — `port/` 직하

포트 **사용법**이 두 플랫폼에서 같아야 하면 확장 함수로 `port/` 직하에 둔다. 인터페이스가 아닌
파일이 섞이지만, 그 대가로 adapter 두 벌이 같은 로직을 베끼지 않는다.

- **ViewModel 이 쓰는 포트 호출은 여기가 아니다.** 그건 그냥 ViewModel 안에서 부른다. 이 자리는
  **adapter 두 벌이 같은 순서로 포트를 때려야 할 때**만 쓴다.

---

## Architecture: MVI

화면 하나는 자기 패키지에 정확히 이 파일들을 갖는다.

| 파일 | 책임 |
|---|---|
| `~Screen.kt` | Composable 진입점 + ViewModel 을 배선하는 stateful 래퍼 |
| `~ViewModel.kt` | `uiState: StateFlow` 와 `eventFlow: Flow` 를 노출하고 intent 를 처리 |
| `~Intent.kt` | 모든 사용자 액션의 sealed 계층 |
| `~UiState.kt` | 화면이 그리는 데 필요한 전부를 담은 불변 data class |
| `~Event.kt` | 일회성 이벤트(네비게이션·토스트 등)의 sealed 계층 |

**ViewModel rules**:
- 코루틴은 `viewModelScope` 로만. `GlobalScope` 금지.
- `uiState` 는 `StateFlow` 로 노출한다(내부는 `MutableStateFlow`).
- `eventFlow` 는 `Flow` 로 노출한다(내부는 `Channel`).
- 모든 사용자 액션은 `sendIntent(intent)` **하나**로 받는다.
- 플랫폼 컨텍스트·뷰를 참조하지 않는다.

**Screen rules**:
- stateful 오버로드가 DI 로 ViewModel 을 얻는다.
- `eventFlow` 를 수집해 네비게이션 람다나 다른 부수효과로 보낸다.
- 렌더링과 상태 관리를 가르기 위해 **stateless 오버로드**를 따로 둔다.

---

## Scaffold

**전체 페이지를 나타내는 Composable(`~Screen`)은 반드시 `Scaffold` 를 쓴다.**

- `innerPadding` 을 루트 컨테이너에 적용한다. 빠뜨리면 edge-to-edge 렌더링이 깨진다.
- `Modifier.padding(0.dp)` 로 우회하거나 무시하지 않는다.
- 전체 페이지가 아닌 Composable(`~View`)은 `Scaffold` 가 필요 없다.

---

## Color

**모든 색은 theme 의 색 정의나 `MaterialTheme.colorScheme.*` 에서 온다. 하드코딩 금지.**

필요한 색이 없으면 **색 정의에 먼저 더하고** 그다음 쓴다.

---

## Preview

**모든 `@Composable` 에 대응하는 `@Preview` 를 둔다.**

- 프리뷰는 앱 테마로 감싼다.
- 프리뷰 함수는 대상 Composable 과 같은 파일에 둔다.
- **멀티플랫폼 `@Preview` 에 파라미터를 붙이지 않는다** — 배경 관련 인자를 받지 않는다. 배경이
  필요하면 테마 안에서 직접 깐다.
- 현실적인 mock 데이터를 준다. `UiState` 가 필요하면 companion 팩토리로 만든다.

---

## Animation

- 애니메이션 Composable 과 헬퍼는 `animation/` 에 둔다.
- **복잡한 애니메이션을 화면·뷰 안에 인라인으로 쓰지 않는다.** `animation/` 의 전용 Composable 로
  빼서 호출한다.
- 단순한 상태 기반 애니메이션은 인라인으로 둬도 된다.

---

## View 분해

Composable 이 복잡해지면 `~View` 로 쪼개 하위 패키지에 둔다.

- 하위 뷰는 **논리적 묶음 이름의 하위 패키지**에 둔다.
- `~View.kt` 한 파일에 주 Composable 하나와 그 프리뷰.
- **뷰 파일이 200줄을 넘기면 더 쪼갠다.**

---

## Navigation

**모든 화면은 `navigation/` 하위 패키지에 대응하는 `~Navigation.kt` 를 갖는다.**

그 파일은 세 가지를 담는다.
1. `@Serializable` 라우트 — 인자가 있으면 `data class`, 없으면 `object`
2. 이 화면으로 가는 `NavController` 확장 — `navigateTo~`
3. 이 화면을 등록하는 `NavGraphBuilder` 확장 — 화면 이름의 camelCase

**Rules**:
- **화면에 `NavController` 를 넘기지 않는다.** 네비게이션 콜백은 평범한 람다다.
- 중첩 그래프에 속하면 자기 `@Serializable` 그래프 라우트를 가진 블록으로 감싼다.
- `enum` 을 라우트 인자로 받으면 **등록부와 라우트 역직렬화 양쪽에** 타입 맵을 넘긴다.
  Navigation 은 기본 타입이 아닌 인자를 리플렉션으로 추론하는데 **Kotlin/Native 에는
  리플렉션이 없고**, 기본 제공 enum 타입은 JVM 전용이다. 빠뜨리면 Android 에서는 통과하고
  iOS 에서만 그래프 생성이 터진다.

### 첫 프레임 깜빡임을 막는 시드

`~UiState` 의 필드가 **비동기 출처**(첫 방출이 첫 프레임 뒤에 오는 것)에서 채워지면서 **그 값을
직전 화면이 이미 알고 있다면**, 라우트 인자로 넘겨 `StateFlow` 의 초기값으로 쓴다. 그러지 않으면
첫 프레임이 기본값을 그리고 실제 값이 도착할 때 "튄다".

**Rules**:
- 라우트 인자는 **시드일 뿐이다.** 비동기 흐름이 진실의 출처로 남아 첫 방출에서 시드를 덮는다 —
  그래서 시드가 틀리거나 없어도 **정확성 버그가 아니고** 깜빡임만 돌아온다. 시드로 로직을
  가르지 않는다.
- 시드는 라우트와 `navigateTo~` 양쪽에 **기본값 있는 마지막 파라미터**로 더한다. 값을 모르는
  호출자(딥링크, 다른 기능에서의 진입)가 계속 컴파일되고 비동기 기본값으로 떨어지게 한다.
- **직전 화면이 이미 손에 들고 있는 값만** 시드한다. 시드를 만들려고 상위에서 데이터를 더 읽거나
  UseCase 를 넓히지 않는다 — 깜빡임을 더 나쁜 결합과 바꾸는 셈이다.

---

## 딥링크와 최상위 네비게이션 — 이 모듈이 소유한다

딥링크 판정과 최상위 그래프는 `commonMain` 에 있다. **두 플랫폼이 같은 주소에 다른 답을 내면 그
자체가 버그이므로 판정에 플랫폼 분기를 두지 않는다.**

| 자리 | 무엇 |
|---|---|
| 상수 | 경로·쿼리 키·주소 조립의 단일 진원지. 주소를 **문자열**로 다룬다 |
| 파서 유틸 | 플랫폼 URI 파서를 대신한다 — 같은 주소를 두 플랫폼이 다르게 읽을 수 있어서다 |
| 주소 파서 | 주소 → 이동 명령 목록. **순수 함수**이고 포트를 주입받지 않는다 |
| 이동 명령 | 최상위 화면 이동 명령 |
| 최상위 그래프 | `NavHost` 와 이동 명령 소비 |

플랫폼에 남는 것은 진입 통로와 OS 배선뿐이다.

> **분기 안에서 포기하면 그때까지 쌓인 명령은 그대로 나간다.** 뿌리 경로가 맞은 뒤에 넣는
> 스택 비우기가 그래서 남는다. 중간 포기 지점을 옮기면 동작이 바뀌므로 파서 테스트가 분기별
> 순서를 고정한다.

새 목적지를 더하는 절차는 `add-deep-link` 스킬이 단일 진원지다.
