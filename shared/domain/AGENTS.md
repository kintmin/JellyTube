## Module: shared:domain

**Role**: 순수 비즈니스 로직 — 도메인 모델, Repository 인터페이스, UseCase. 플랫폼 의존 없음.

---

## 여기 두는 것

- **도메인 모델**: 핵심 개념을 나타내는 불변 data class. 직렬화 애너테이션을 붙이지 않는다.
  **Repository 경계를 넘어 돌아오는 결과·출력 타입도 도메인 데이터**이므로 여기 속한다.
- **Repository 인터페이스**: 무엇이 필요한지만 선언하고, 어떻게 저장하는지는 규정하지 않는다.
- **UseCase**: 책임 하나를 갖고 `operator fun invoke(...)` 로 들어가는 클래스.

## 여기 두지 않는 것

- Repository 구현 → `shared:data`
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
