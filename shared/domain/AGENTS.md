## Module: shared:domain

**Role**: 순수 비즈니스 로직 — 도메인 모델, Repository 인터페이스, UseCase. 플랫폼 의존 없음.

---

## 여기 두는 것

- **도메인 모델**: 핵심 개념을 나타내는 불변 data class. 직렬화 애너테이션을 붙이지 않는다.
  **Repository 경계를 넘어 돌아오는 결과·출력 타입도 도메인 데이터**이므로 여기 속한다.
- **Repository 인터페이스**: 무엇이 필요한지만 선언하고, 어떻게 저장하는지는 규정하지 않는다.
- **UseCase**: 책임 하나를 갖고 `operator fun invoke(...)` 로 들어가는 클래스.
- **포트**: 도메인 로직이 바깥의 **실행 수단**을 필요로 할 때 뚫는 인터페이스 — *언제·어디서
  돌지* 를 플랫폼이 정하는 일(작업 예약, 실행 유지 같은 것)이다. 선언만 여기 두고 구현은
  앱·조립 모듈이 채운다. **판정이 아니라 실행 수단일 때만 포트다** — 무엇을 할지 정하는 것은
  UseCase 가 하고, 그것을 돌려 주는 수단만 포트로 뚫는다.

## 여기 두지 않는 것

- Repository 구현 → `shared:data`
- 포트 구현 → 앱·조립 모듈. 이 모듈은 인터페이스만 갖는다
- Android·iOS SDK 의존 → 이 모듈에서 금지
- 코루틴 스코프 생성 → UseCase 는 스코프를 소유하지 않는다. 수명은 호출자가 쥔다
- DI 배선 → `shared:data` 또는 앱 모듈

---

## UseCase 규약

- 한 파일에 하나, `~UseCase.kt`.
- 의존하는 Repository 인터페이스를 생성자로 받는다.
- `operator fun invoke(...)` 가 유일한 공개 입구다.
- 스트림은 `Flow`, 일회성 작업은 `suspend fun`.
- **도메인 사유로 에러 타입을 바꾸는 경우를 빼면 예외를 잡지 않는다.** 실패는 호출자가 다룬다.
- **UseCase 는 상태를 갖지 않는다.** 주입이 factory 라서 필드는 주입마다 초기화된다 — 호출자가
  우연히 한 인스턴스를 들고 있을 때만 동작하는 코드가 된다. 진행 중 표시 같은 상태는 싱글턴
  Repository 가 갖고, 호출자별 상태는 호출자가 들고 넘긴다.

---

## 취소 규율 — `runCatching` 은 취소를 삼킨다

`runCatching` 은 `CancellationException` 까지 `Result.failure` 로 감싼다. 그래서 코루틴을 취소해도
실패로 기록되고, 바깥 코루틴은 자기가 취소된 줄 모른 채 다음 줄을 이어서 실행한다.

**suspend 하는 블록을 `Result` 로 감쌀 때는 `suspendRunCatching` 이다.** `runCatching` 이 맞는 자리는
둘뿐이다.

- **블록이 동기일 때.** 블록 안에서 취소가 날 수 없다 — enum 파싱, C 인터옵 콜백 같은 것.
- **`withContext(NonCancellable)` 안의 정리 코드일 때.** 취소를 다시 던지면 정리 경로 밖으로 샌다.

`withContext(Dispatchers.IO) { ... }` 는 동기가 아니다 — 그 안에서 suspend 를 부르면 취소가 난다.

---

## Repository 인터페이스 규약

- 도메인 개념 하나당 인터페이스 하나.
- 스트림은 `Flow`, 일회성 변경은 `suspend fun`.
- **DB·네트워크·파일시스템 타입을 참조하지 않는다.** 도메인 모델만 주고받는다.
- 인터페이스 파일에는 **인터페이스만** 둔다. 메서드가 새 결과 타입을 돌려준다면 그 타입은
  `model/` 의 자기 파일로 간다 — `model/` 은 **데이터 모양**, `repository/` 는 그것을 주고받는
  **계약**이다.

---

## 모델 배치 규약

- `<feature>/model/` 이 **모든 도메인 data class 의 유일한 집**이다. 핵심 개념과, Repository
  경계를 넘는 결과·출력 래퍼가 함께 여기 있다. 한 파일에 한 클래스, 이름은 클래스명 그대로.
- Repository 인터페이스 파일 안에 도메인 data class 를 함께 선언하지 않는다.
- UseCase 하나에만 쓰이는 결과 타입은 그 UseCase 파일에 둘 수 있다. 다만 Repository 인터페이스가
  그것을 돌려주게 되는 순간 `model/` 로 옮긴다.
- 데이터 계층의 DTO(DB entity, facade 래퍼, 네트워크 DTO)는 여기 오지 않는다 — `shared:data` 다.
