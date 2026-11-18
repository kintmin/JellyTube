---
name: room-entity-migration
description: JellyTube의 Room DB에 entity/컬럼 변경을 규약대로 추가하고 마이그레이션을 강제한다. 스키마 버전업 시 Migration 작성과 양쪽 플랫폼 빌더 등록. "테이블 추가", "컬럼 추가", "DB 스키마 변경", "Room entity" 요청 시 사용.
---

# Room Entity & Migration

**이 스킬이 있는 이유는 틀렸을 때 사용자 데이터가 날아가서다.** 컴파일도 테스트도 통과한 뒤
기존 설치본에서만 터지므로, 체크리스트로 막는다. 배치 규약은 `shared/data/AGENTS.md` 가 단일
진원지이고, 작성 전 기존 entity 하나와 DB 선언을 읽어 현재 버전을 확인한다.

## 핵심 규칙 (위반 시 런타임 크래시)

- **스키마를 바꾸면(테이블·컬럼 추가·변경·삭제) 버전을 올리고 반드시 마이그레이션을 제공한다.**
  버전만 올리고 두면 기존 사용자 앱이 크래시한다. 데이터를 버리는 파괴적 마이그레이션은 명시적
  합의 없이는 금지다.
- **entity 는 모듈 밖으로 내보내지 않는다** — 매퍼에서 도메인 모델로 바꿔서만 나간다.
- 마이그레이션은 **양쪽 플랫폼 DB 빌더에 모두 등록**한다. 한쪽만 등록하면 그 플랫폼에서만 터진다.

## 절차

1. **entity** — 배치는 `shared/data/AGENTS.md` 를 따른다. nullable·기본값을 마이그레이션 SQL 과
   일치시킨다.
2. **DAO** — 쿼리는 `Flow` 또는 `suspend`. 메인스레드 쿼리 금지. 문자열을 이어 만든 쿼리 금지
   (바인딩 파라미터를 쓴다).
3. **facade** — 여러 DAO 를 한 트랜잭션으로 묶어야 할 때만 만든다.
4. **매퍼** — entity ↔ 도메인 모델 변환. 도메인 모델은 `shared:domain` 소유다.
5. **DB 등록과 버전업** — entity 목록에 더하고 버전을 올린다. 스키마를 export 하면 생성된
   JSON 도 함께 커밋한다.
6. **마이그레이션 작성** — 정확한 `ALTER TABLE` / `CREATE TABLE` SQL 을 쓰고 양쪽 빌더에 등록한다.
7. **DI** — 새 DAO 와 facade 를 data 의 공통 DI 모듈에 등록한다.

## 마이그레이션 체크리스트

- [ ] 컬럼 추가: `NOT NULL` 이면 `DEFAULT` 가 **반드시** 있어야 한다
- [ ] 테이블 추가: entity 정의와 **타입·제약·인덱스까지 100% 일치**하는 `CREATE TABLE`
- [ ] 외래키·인덱스 변경도 마이그레이션에 반영했는지
- [ ] **양쪽 플랫폼 빌더에 등록**했는지
- [ ] 이전 버전 설치본에서 올려 보는 시나리오를 점검했는지(크래시 없이 데이터 보존)

## 완료 확인

1. `./gradlew.bat :shared:data:compileAndroidMain` — KSP/Room 컴파일러 통과.
2. `./gradlew.bat :shared:data:testAndroidHostTest` — 가능하면 N→N+1 마이그레이션 테스트를 더한다.
3. 모든 파일 UTF-8(BOM 없음).
