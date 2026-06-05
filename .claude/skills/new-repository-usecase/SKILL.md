---
name: new-repository-usecase
description: JellyTube에 새 도메인 데이터 흐름을 shared:domain(모델/Repository 인터페이스/UseCase)과 shared:data(RepositoryImpl + Koin 등록)에 걸쳐 규약대로 추가한다. UseCase는 factoryOf로 DI(싱글톤 금지). "리포지토리 추가", "유즈케이스 만들어줘", "새 데이터 흐름 추가" 요청 시 사용.
---

# New Repository + UseCase

도메인 데이터 흐름은 `shared:domain`(인터페이스/UseCase)과 `shared:data`(구현/DI) 두 모듈에 걸친 절차다. 단일 진원지는 `shared/domain/AGENTS.md`와 `shared/data/AGENTS.md`이며, 작성 전 둘 다 읽고 기존 feature(예: `audio_track`)의 패키지 구조를 참고한다.

## 입력 확인
- 도메인 개념 이름 (예: `Playlist`, `AudioTrack`) → feature 패키지명(snake_case)
- 필요한 메서드 (스트림인지 일회성 mutation인지)
- 데이터 출처 (Room DAO / 네트워크 / DataStore / 파일)

## 절차 (모듈·순서 고정)

### shared:domain (`com.kintmin.domain.<feature>/`)
1. **모델** — `model/`에 불변 data class. **직렬화 애너테이션 금지**(순수 도메인).
2. **Repository 인터페이스** — `repository/<Concept>Repository.kt`. 스트림은 `Flow<T>` 반환, mutation은 `suspend fun`. DB/네트워크/파일 타입 참조 금지 — 도메인 모델만.
3. **UseCase** — `usecase/<Verb><Concept>UseCase.kt`. 1파일 1책임, `operator fun invoke(...)` 단일 진입. 생성자에 Repository 인터페이스 주입. `CoroutineScope` 생성 금지(호출자가 수명 제어). 도메인 사유 없으면 예외 catch 금지.

### shared:data (`com.kintmin.data/`)
4. **RepositoryImpl** — `repository_impl/<Concept>RepositoryImpl.kt`. domain 인터페이스 구현. DAO facade / 네트워크 datasource / DataStore 주입(가능하면 `Context` 직접 주입 회피). 가능하면 `commonMain`, 플랫폼 드라이버 필요할 때만 `androidMain`/`iosMain`.
5. **Koin 등록** — `di/DataCommonModule.kt`에 추가:
   - Repository: `singleOf(::<Concept>RepositoryImpl) bind <Concept>Repository::class`  (single = 앱 전역 1개)
   - **UseCase: `factoryOf(::<Verb><Concept>UseCase)`**  ← **반드시 factory. `single`/`singleOf` 금지.** UseCase는 상태 없는 일회용 객체이므로 매 호출마다 새 인스턴스를 받는다(기존 컨벤션과 일치).

## 강제 규칙
- 의존성 방향: `shared:domain`은 아무것도 의존하지 않음. `shared:data`가 domain 인터페이스를 구현. presentation/iOS는 domain 인터페이스만 알고 구현체는 DI로 주입.
- DB entity를 모듈 밖으로 노출 금지 — `local_db/mapper/`에서 도메인 모델로 매핑.
- UseCase DI는 `factoryOf`(비싱글톤). Repository DI는 `singleOf`.
- 모든 파일 UTF-8(BOM 없음), 한글 주석/문자열 보존.

## 완료 확인
1. `./gradlew.bat :shared:domain:compileKotlin :shared:data:compileDebugKotlin`.
2. Koin 그래프가 새 UseCase를 해석하는지(런타임 누락은 컴파일로 안 잡힘) — 사용하는 ViewModel/Client에서 주입 확인.
3. shared API가 바뀌었으면 iOS는 프레임워크 재빌드 필요(`build-verifier`로 검증, macOS 한정).

## 골격 예시
```kotlin
// shared:domain — usecase/FetchPlaylistListUseCase.kt
class FetchPlaylistListUseCase(
    private val repository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> = repository.fetchAll()
}
```
```kotlin
// shared:data — di/DataCommonModule.kt (발췌)
singleOf(::PlaylistRepositoryImpl) bind PlaylistRepository::class
factoryOf(::FetchPlaylistListUseCase)   // UseCase는 factory (싱글톤 아님)
```
