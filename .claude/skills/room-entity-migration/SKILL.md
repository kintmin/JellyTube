---
name: room-entity-migration
description: JellyTube의 shared:data Room DB에 entity/컬럼 변경을 규약대로 추가하고 마이그레이션을 강제한다. entity/DAO/facade/mapper 배치 + 스키마 버전업 시 Migration 작성. "테이블 추가", "컬럼 추가", "DB 스키마 변경", "Room entity" 요청 시 사용.
---

# Room Entity & Migration

Room DB 변경은 `shared:data`의 `local_db/` 안에서 이뤄진다. 단일 진원지는 `shared/data/AGENTS.md`이며, 작성 전 기존 entity(`AudioMediaEntity` 등)와 `JellyTubeDatabase`를 읽어 스타일·버전을 확인한다.

## 핵심 규칙 (위반 시 런타임 크래시)
- **스키마를 바꾸면(테이블/컬럼 추가·변경·삭제) DB 버전을 올리고 반드시 `Migration`을 제공한다.** 버전만 올리고 마이그레이션 없이 두면 기존 사용자 앱이 크래시한다. `fallbackToDestructiveMigration`은 사용자 데이터를 날리므로 명시적 합의 없이는 금지.
- **entity는 이 모듈 밖으로 노출 금지** — `local_db/mapper/`에서 도메인 모델로 매핑해서만 내보낸다.
- entity는 `local_db/model/`, DAO는 `local_db/dao/`, 여러 DAO를 조합하는 facade는 `local_db/dao_facade/`.

## 절차 (`com.kintmin.data.local_db/`)
1. **entity** — `model/<Name>Entity.kt`. `@Entity`(필요 시 `tableName`, `indices`, `foreignKeys`). nullable/기본값은 마이그레이션 SQL과 일치시킨다.
2. **DAO** — `dao/<Name>Dao.kt`. 쿼리는 `Flow<T>` 또는 `suspend fun`. **메인스레드 쿼리 금지**. 원시 문자열 연결 쿼리 금지(SQL injection) — 바인딩 파라미터 사용.
3. **facade** (여러 DAO 조합이 필요할 때) — `dao_facade/<Name>Facade.kt`.
4. **mapper** — `mapper/`에 entity ↔ 도메인 모델 변환. 도메인 모델은 `shared:domain` 소유.
5. **DB 등록 + 버전업** — `local_db/database/JellyTubeDatabase.kt`의 `@Database(entities = [...], version = N+1)`에 entity 추가/버전 증가. `exportSchema`가 켜져 있으면 생성된 스키마 JSON도 커밋.
6. **Migration 작성** — `Migration(N, N+1)`에 정확한 `ALTER TABLE`/`CREATE TABLE` SQL. DB 빌더(`JellyTubeDatabaseBuilder.android.kt` / `.ios.kt`)에 `.addMigrations(...)` 등록.
7. **DI** — 새 DAO는 `di/DataCommonModule.kt`에 `single { get<JellyTubeDatabase>().<name>Dao() }` 추가, facade는 `singleOf(::<Name>Facade)`.

## 마이그레이션 체크리스트
- [ ] 컬럼 추가: `ALTER TABLE x ADD COLUMN col TYPE NOT NULL DEFAULT ...` (NOT NULL이면 DEFAULT 필수)
- [ ] 테이블 추가: entity 정의와 100% 일치하는 `CREATE TABLE` (타입/제약/인덱스)
- [ ] foreign key / index 변경도 마이그레이션에 반영
- [ ] android/iOS 양쪽 빌더에 migration 등록(공통이면 commonMain에서 공유)

## 완료 확인
1. `./gradlew.bat :shared:data:compileDebugKotlin` (KSP/Room 컴파일러 통과).
2. `:shared:data:testDebugUnitTest` — 가능하면 `MigrationTestHelper`로 N→N+1 마이그레이션 테스트.
3. 구버전 설치본에서 업그레이드 시나리오를 점검(크래시 없이 데이터 보존).
- 모든 파일 UTF-8(BOM 없음), 한글 주석/문자열 보존.
