# 테스트 작성 가이드 (Living Doc)

블로그 수집기 백엔드의 단위/통합 테스트를 **누락 없이** 채우고, 이후에도 같은 규칙으로 추가하기 위한 운영 문서입니다.  
대상 모듈: `user-service`, `integrated-api`, `integrated-worker`, `interaction-service` (+ 필요 시 common).

> **체크리스트 단일 스키마 (필수):**  
> `ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일`  
> 요약 표·컬럼 생략 금지. 외부 노션/시트와 동일 컬럼으로 맞춘다.

레이어·상태 값:

| 컬럼 | 허용 값 |
|------|---------|
| Validator / Service / Controller/Worker | `Done` · `Partial` · `Todo` · `N/A` |
| 우선순위 | `P0` · `P1` · `P2` · `P3` |
| 상태 | `Todo` · `In progress` · `Done` · `Blocked` |

### Done 판정 (Jacoco LINE 커버리지)

**기준:** 해당 레이어에 매핑된 **프로덕션 클래스**의 Jacoco **LINE covered ratio ≥ 80%**.

**측정 범위:** **Service 계층부터** (Controller / Validator / Service / Worker·Runner / Util 등).  
**제외:** Repository(`**/repository/**`), DTO, config, `*Application`, QueryDSL `Q*`.

| 레이어 값 | 의미 |
|-----------|------|
| `Done` | 매핑 클래스 LINE ≥ 80% |
| `Partial` | 테스트는 있으나 매핑 클래스 LINE **&lt; 80%** |
| `Todo` | 유의미한 테스트 없음(대개 ~0%) 또는 미작성 |
| `N/A` | 해당 레이어 없음 |

행 `상태=Done` 조건: 필요한 레이어 칸이 모두 `Done` 또는 `N/A`.

측정:

```bash
# 권장: 4개 서비스(또는 지정 모듈) Jacoco LINE ≥ 80% 한 번에 확인
./scripts/check-coverage.sh
./scripts/check-coverage.sh user-service

# 모듈 단위
./gradlew :user-service:test :user-service:jacocoTestReport
./gradlew :user-service:jacocoTestCoverageVerification
# HTML: user-service/build/reports/jacoco/test/html/index.html
```

리포트에서 제외: `**/repository/**`, `**/Q*.class`, `**/*Application.class`, `**/dto/**`, `**/config/**`.  
현황 표(기능 행)는 구글 시트에서 관리하고, **지금 80%인가?** 는 위 스크립트/Gradle이 단일 확인 수단이다.

ID 접두어: `CM`(common) · `US` · `IA` · `IW` · `IS` — Phase는 ID에 넣지 않고 컬럼으로 관리.

---

## 0. 한눈에 보는 진행 순서

```text
Phase 0  공통 헬퍼·컨벤션 정리
   ↓
Phase 1  user-service          (JWT·유저 규칙 기준)
   ↓
Phase 2  integrated-api        (수집/색인 Job 입구)
   ↓
Phase 3  integrated-worker     (Job 소비·크롤·인덱싱)
   ↓
Phase 4  interaction-service   (검색·상호작용·조회수 sync)
   ↓
Phase 5  (선택) DB / ES / Redis / Security 통합 테스트
```

모듈 안 레이어 순서:

```text
① Validator  →  ② Service / Executor / Picker / Worker  →  ③ Controller  →  ④ (선택) IT
```

---

## 1. 목표와 범위

### 1.1 목표

- OpenAPI·도메인 클래스 기준으로 **기능 누락 없이** 테스트 골격 확보
- 새 기능 PR마다 같은 레이어 패턴으로 테스트 추가
- 느리고 깨지기 쉬운 통합 테스트는 소수로 유지, 기본은 **빠른 단위 테스트**

### 1.2 현재 레포 기본 스타일 (유지)

| 항목 | 현황 |
|------|------|
| 프레임워크 | JUnit 5 + Mockito + AssertJ |
| Spring 테스트 슬라이스 | 거의 사용 안 함 (`@SpringBootTest` / `@WebMvcTest` 없음) |
| Controller | MockMvc `standaloneSetup` |
| 인증 | 필터 체인 대신 principal mock resolver 주입 |
| 네이밍 | `@Nested` + **테스트 메서드명은 한국어** (`~할_수_있다` 스타일). 클래스/`@Nested`에 `@DisplayName` |
| 패키지 | 프로덕션과 동일 패키지 미러 (`.../postlike/service/PostLikeServiceTest`) |
| IT / Testcontainers | 없음 (Phase 5에서 선택 도입) |

### 1.3 서비스 결합 방식 (테스트 설계에 영향)

| 결합 | 메커니즘 |
|------|----------|
| 인증 | user-service가 JWT 발급, 다른 API가 동일 `jwt.*`로 검증 |
| 수집/색인 | integrated-api가 Job 기록 → integrated-worker가 DB 폴링 실행 |
| 검색 | worker가 ES 색인 → interaction-service / integrated-api가 조회 |
| 유저 참조 | interaction-service가 공유 PostgreSQL의 `User` 조회 (user-service HTTP 아님) |

---

## 2. 컨벤션

### 2.1 테스트 종류와 책임

| 종류 | 검증 대상 | 도구 | 필수 여부 |
|------|-----------|------|-----------|
| Validator | 입력·상태·중복·권한 규칙 | 순수 단위 / Mockito | 권장 |
| Service / Worker | 분기, 상태 전이, 멱등, 실패 처리 | Mockito | **필수** |
| Controller | 매핑, 상태코드, principal 전달 | MockMvc standalone | API 모듈 **필수** |
| IT (`*IT`) | Flyway, QueryDSL, ES mapping, Redis flush, Security chain | Testcontainers 등 | 선택 (Phase 5) |

### 2.2 파일·클래스 규칙

- 위치: `src/test/java` 아래 프로덕션과 같은 패키지
- 이름: `{ClassName}Test` (통합은 `{ClassName}IT`)
- 구조 예:

```java
@DisplayName("PostLikeService 테스트")
@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Nested
    @DisplayName("좋아요 추가")
    class AddLike {
        @Test
        void 이미_좋아요한_게시글이면_Conflict를_던진다() { ... }
    }
}
```

### 2.3 기능 누락 방지 체크 소스 (3곳)

1. **OpenAPI 스냅샷** — [`docs/api/`](./api/README.md)
2. **도메인 클래스** — `*Service`, `*Worker`, `*Validator`, `*Executor`, `*Picker`, `*Runner`
3. **스케줄/부트스트랩/외부 클라이언트** — API에 안 보이는 기능

행 추가 시 **반드시** 아래 스키마(문서 상단과 동일):

| ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일 |
|----|-------|------|--------|--------|---------|-----------|---------|-------------------|----------|------|------|--------|
| US-001 | 1 | user-service | auth | password login | `POST /auth/v1/auth/login` | Done | Done | Done | P0 | Done | | 2026-08-10 |

완료 판정 (레이어 조합 + Jacoco):

- OpenAPI → Validator(없으면 N/A) + Service + Controller 가 각각 Jacoco 기준으로 `Done`/`N/A`
- Worker/Scheduler/Runner → Service(또는 동등) + Worker/Runner 클래스 LINE ≥ 80%
- Util → 해당 util 클래스 LINE ≥ 80% (Controller `N/A`)
- **테스트 파일만 있고 커버리지 미달이면 `Partial`** (파일 존재 ≠ Done)

### 2.4 공통 헬퍼 (testFixtures)

**결정: 옵션 A — `common-data-access` testFixtures 유지** (`2026-08-10`)

| 옵션 | 설명 | 결과 |
|------|------|------|
| **A. testFixtures 유지** | 기존 경로 유지, resolver만 정리 | **채택** |
| B. 모듈 로컬 헬퍼 | 각 서비스 `src/test/.../support`에 복제 | 기각 |
| C. `test-support` 모듈 | 별도 java-library | 기각 |

- `MockJwtPrincipalResolver` — `user()` / `admin()` / `UserType` 생성자
- 소비: `user-service`, `integrated-api`, `interaction-service`
- 불필요: `integrated-worker`
- 보류: 엔티티 공용 빌더, anonymous(`required=false`) resolver

---

## 3. Phase 0 — 공통 준비

| ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일 |
|----|-------|------|--------|--------|---------|-----------|---------|-------------------|----------|------|------|--------|
| CM-001 | 0 | common | helpers | 헬퍼 전략 결정 | testing-guide §2.4 | N/A | N/A | N/A | P0 | Done | 옵션 A (testFixtures 유지) | 2026-08-10 |
| CM-002 | 0 | common-data-access | helpers | MockJwtPrincipalResolver USER/ADMIN | `MockJwtPrincipalResolver` | N/A | N/A | N/A | P0 | Done | `user()` / `admin()` | 2026-08-10 |
| CM-003 | 0 | common | helpers | 엔티티 공용 빌더 | testFixtures builders | N/A | N/A | N/A | P2 | Todo | 중복 심해지면 추가 | 2026-08-10 |
| CM-004 | 0 | common | convention | 테스트 컨벤션 고정 | testing-guide §2 | N/A | N/A | N/A | P0 | Done | Mockito + standalone + 한글 메서드명 | 2026-08-10 |
| CM-005 | 0 | common | helpers | anonymous CurrentUser mock | `@CurrentUser(required=false)` | N/A | N/A | N/A | P2 | Todo | interaction Phase 4 때 | 2026-08-10 |

실행:

```bash
./gradlew test
./gradlew :user-service:test :integrated-api:test :integrated-worker:test :interaction-service:test
```

---

## 4. 모듈 안 레이어 작성 순서 (공통)

```text
1) *ValidatorTest
2) *ServiceTest
3) *ControllerTest
4) *WorkerTest / *RunnerTest
```

Controller 패턴:

```java
mockMvc = MockMvcBuilders.standaloneSetup(controller)
    .setCustomArgumentResolvers(new MockJwtPrincipalResolver())
    .build();
```

외부 I/O(Google, Selenium, ES, Redis)는 경계에서 mock. 실연동은 Phase 5.

---

## 5. Phase 1 — user-service

**역할:** JWT 발급, 회원가입/계정 병합, admin bootstrap.  
**의존:** `common-data-access`, `common-web` (Flyway + Postgres).

### 5.1 Jacoco 측정 결과 (`2026-08-10`)

명령: `./gradlew :user-service:test :user-service:jacocoTestReport`  
게이트: `./gradlew :user-service:jacocoTestCoverageVerification` (LINE ≥ 0.80)

| 범위 | LINE | BRANCH | 판정 |
|------|------|--------|------|
| **user-service 모듈** (Repository 제외) | **83.9%** (312/372) | 76.3% | **통과** (`jacocoTestCoverageVerification` OK) |

클래스별 LINE (Repository 제외 후):

| 클래스 | LINE | 레이어 Done? |
|--------|------|----------------|
| `AuthController` | 100% | Done |
| `UserController` | 100% | Done |
| `AuthValidator` | 100% | Done |
| `UserValidator` | 100% | Done |
| `UserAuthenticationValidator` | 96.9% | Done |
| `AuthService` | 87.0% | Done |
| `UserService` | 100% | Done |
| `UserAuthenticationService` | 100% | Done |
| `JwtAuthenticationTokenIssuer` | 100% | Done |
| `NicknameGenerateUtil` | 100% | Done |
| `AdminBootstrapRunner` | 84.2% | Done |
| `GoogleOAuthTokenClient` | 4.5% | Todo |
| `GoogleIdTokenVerifierService` | 3.1% | Todo |

Repository(`UserQueryRepository` 등)는 **측정 대상 아님** (단위 테스트에서 mock, IT는 Phase 5).

### 5.2 체크리스트 (Jacoco 80% 재계산)

| ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일 |
|----|-------|------|--------|--------|---------|-----------|---------|-------------------|----------|------|------|--------|
| US-001 | 1 | user-service | auth | password login | `POST /auth/v1/auth/login` | Done | Done | Done | P0 | Done | AuthValidator 100% / AuthService 87% / AuthController 100% | 2026-08-10 |
| US-002 | 1 | user-service | auth | Google OAuth callback | `GET /auth/v1/oauth/google/callback` | Done | Done | Done | P0 | Done | AuthService 기준. Google HTTP는 US-013 | 2026-08-10 |
| US-003 | 1 | user-service | user | signup | `POST /user/v1/users` | Done | Done | Done | P0 | Done | UserValidator 100% / **UserService 100%** / UserController 100% | 2026-08-10 |
| US-004 | 1 | user-service | user | get me | `GET /user/v1/users/me` | Done | Done | Done | P0 | Done | UserService 100% | 2026-08-10 |
| US-005 | 1 | user-service | user | update me | `PATCH /user/v1/users/me` | Done | Done | Done | P0 | Done | UserService 100% | 2026-08-10 |
| US-006 | 1 | user-service | user | change password | `PATCH /user/v1/users/me/password` | Done | Done | Done | P0 | Done | UserService 100% | 2026-08-10 |
| US-007 | 1 | user-service | user | merge oauth | `POST /user/v1/users/me/merge-oauth` | Done | Done | Done | P0 | Done | UserService 100% | 2026-08-10 |
| US-008 | 1 | user-service | user | delete me | `DELETE /user/v1/users/me` | Done | Done | Done | P0 | Done | UserService 100% | 2026-08-10 |
| US-009 | 1 | user-service | userauthentication | create/read/update/merge/delete | `UserAuthenticationService` | Done | Done | N/A | P0 | Done | Validator 96.9% / Service 100% | 2026-08-10 |
| US-010 | 1 | user-service | bootstrap | admin bootstrap | `AdminBootstrapRunner` | N/A | Done | Done | P0 | Done | Runner LINE 84.2% | 2026-08-10 |
| US-011 | 1 | user-service | utils | random nickname | `NicknameGenerateUtil` | N/A | Done | N/A | P1 | Done | 100% | 2026-08-10 |
| US-012 | 1 | user-service | auth | JWT issue | `JwtAuthenticationTokenIssuer` | N/A | Done | N/A | P0 | Done | 100% | 2026-08-10 |
| US-013 | 1 | user-service | auth | Google OAuth HTTP clients | `GoogleOAuthTokenClient` / `GoogleIdTokenVerifierService` | N/A | Todo | N/A | P3 | Todo | LINE 4.5% / 3.1%. Phase 5 또는 mock 유지 | 2026-08-10 |

**Phase 1 요약:** 모듈 LINE **83.9% ≥ 80%** (Repository 제외) → 모듈 게이트 통과.  
US-001~012 Done. US-013(Google HTTP)만 Todo(Phase 5 / mock 유지) — 모듈 합산은 이미 80% 이상.

다음 액션:

1. Phase 2로 진행 (동일: Repository 제외 + Jacoco 80%)
2. US-013은 Phase 5 백로그

---

## 6. Phase 2 — integrated-api

**역할:** 관리자용 provider / collect source / job / indexed post / ES index 관리.  
**의존:** shared DB + ES.

| ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일 |
|----|-------|------|--------|--------|---------|-----------|---------|-------------------|----------|------|------|--------|
| IA-001 | 2 | integrated-api | provider | list providers | `GET /collect/v1/providers` | Partial | Done | Done | P1 | In progress | Validator 직접 테스트 미흡 | 2026-08-10 |
| IA-002 | 2 | integrated-api | provider | create provider | `POST /collect/v1/providers` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-003 | 2 | integrated-api | provider | get provider | `GET /collect/v1/providers/{id}` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-004 | 2 | integrated-api | provider | update provider | `PATCH /collect/v1/providers/{id}` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-005 | 2 | integrated-api | provider | delete provider | `DELETE /collect/v1/providers/{id}` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-006 | 2 | integrated-api | collectsource | list sources | `GET /collect/v1/sources` | Partial | Done | Done | P1 | In progress | CRON/MANUAL 규칙 | 2026-08-10 |
| IA-007 | 2 | integrated-api | collectsource | create source | `POST /collect/v1/sources` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-008 | 2 | integrated-api | collectsource | get source | `GET /collect/v1/sources/{id}` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-009 | 2 | integrated-api | collectsource | update source | `PATCH /collect/v1/sources/{id}` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-010 | 2 | integrated-api | collectsource | delete source | `DELETE /collect/v1/sources/{id}` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-011 | 2 | integrated-api | collectingjob | start collect | `POST /collect/v1/sources/{source-id}/_start` | Partial | Done | Done | P0 | In progress | active conflict | 2026-08-10 |
| IA-012 | 2 | integrated-api | collectingjob | stop collect | `POST /collect/v1/sources/{source-id}/_stop` | Partial | Done | Done | P0 | In progress | | 2026-08-10 |
| IA-013 | 2 | integrated-api | collectingjob | list jobs | `GET /collect/v1/jobs` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-014 | 2 | integrated-api | collectingjob | get job | `GET /collect/v1/jobs/{id}` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-015 | 2 | integrated-api | collectsourcepost | get posting | `GET /collect/v1/postings/{id}` | Partial | Done | Done | P2 | In progress | 읽기 위주 | 2026-08-10 |
| IA-016 | 2 | integrated-api | indexingjob | reindex by source | `POST /index/v1/sources/{source-id}/_reindex` | Partial | Done | Done | P0 | In progress | | 2026-08-10 |
| IA-017 | 2 | integrated-api | indexingjob | reindex by post | `POST /index/v1/posts/{post-id}/_reindex` | Partial | Done | Done | P0 | In progress | | 2026-08-10 |
| IA-018 | 2 | integrated-api | indexingjob | list jobs | `GET /index/v1/jobs` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-019 | 2 | integrated-api | indexingjob | get job | `GET /index/v1/jobs/{id}` | Partial | Done | Done | P1 | In progress | | 2026-08-10 |
| IA-020 | 2 | integrated-api | indexedpost | get indexed posting | `GET /index/v1/postings/{posting-id}` | Todo | Todo | Todo | P0 | Todo | **빈 구멍** | 2026-08-10 |
| IA-021 | 2 | integrated-api | elasticsearchindex | bootstrap | `POST /index/v1/elasticsearch/_bootstrap` | N/A | Todo | Todo | P0 | Todo | **빈 구멍** | 2026-08-10 |
| IA-022 | 2 | integrated-api | elasticsearchindex | reindex alias | `POST /index/v1/elasticsearch/_reindex` | N/A | Todo | Todo | P0 | Todo | **빈 구멍** | 2026-08-10 |
| IA-023 | 2 | integrated-api | elasticsearchindex | status | `GET /index/v1/elasticsearch/status` | N/A | Todo | Todo | P0 | Todo | **빈 구멍** | 2026-08-10 |
| IA-024 | 2 | integrated-api | elasticsearchindex | auto bootstrap runner | `ElasticsearchAutoBootstrapRunner` | N/A | Todo | Todo | P1 | Todo | Controller/Worker=Runner | 2026-08-10 |

이 Phase 권장 순서: Validator 직접화 → IndexedPost → ES admin/bootstrap.

---

## 7. Phase 3 — integrated-worker

**역할:** Collecting/Indexing Job 폴링, 크롤, Post 적재, ES 색인, stale 복구.  
**의존:** shared DB + ES + Selenium. REST Controller 없음 → Controller/Worker 칸에 Worker/Picker/Executor 기입.

| ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일 |
|----|-------|------|--------|--------|---------|-----------|---------|-------------------|----------|------|------|--------|
| IW-001 | 3 | integrated-worker | collectingjob | cron job 생성 | `CollectingJobCronCreationWorker` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| IW-002 | 3 | integrated-worker | collectingjob | pending pick | `CollectingJobPicker` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| IW-003 | 3 | integrated-worker | collectingjob | poll → executeAsync | `CollectingJobWorker` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| IW-004 | 3 | integrated-worker | collectingjob | execute success/fail | `CollectingJobExecutor` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| IW-005 | 3 | integrated-worker | collectingjob | status transitions | `CollectingJobService` | Partial | Done | N/A | P0 | In progress | Validator 직접화 여지 | 2026-08-10 |
| IW-006 | 3 | integrated-worker | collectsource | active CRON sources | `CollectSourceService` | N/A | Done | N/A | P1 | Done | | 2026-08-10 |
| IW-007 | 3 | integrated-worker | collectsourcepost | hash/persist/recover | `CollectSourcePostService` | Partial | Done | N/A | P0 | In progress | | 2026-08-10 |
| IW-008 | 3 | integrated-worker | post | createPostsIfAbsent | `PostService` | N/A | Done | N/A | P0 | Done | | 2026-08-10 |
| IW-009 | 3 | integrated-worker | indexingjob | pending/cron pick | `IndexingJobPicker` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| IW-010 | 3 | integrated-worker | indexingjob | poll worker | `IndexingJobWorker` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| IW-011 | 3 | integrated-worker | indexingjob | execute success/fail | `IndexingJobExecutor` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| IW-012 | 3 | integrated-worker | indexingjob | counts/success/fail | `IndexingJobService` | N/A | Done | N/A | P0 | Done | | 2026-08-10 |
| IW-013 | 3 | integrated-worker | indexingjob | CRON/MANUAL index order | `IndexingService` | N/A | Done | N/A | P0 | Done | TX→ES→TX | 2026-08-10 |
| IW-014 | 3 | integrated-worker | indexingjob | stale INDEXING recovery | `IndexingReconciliationWorker` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| IW-015 | 3 | integrated-worker | crawler | BlogCrawlerService + strategy | `BlogCrawlerService` | N/A | Todo | N/A | P0 | Todo | **빈 구멍** | 2026-08-10 |
| IW-016 | 3 | integrated-worker | crawler | Toss HTML parser | `TossPostParser` | N/A | Todo | N/A | P0 | Todo | HTML fixture | 2026-08-10 |
| IW-017 | 3 | integrated-worker | crawler | Line HTML parser | `LinePostParser` | N/A | Todo | N/A | P0 | Todo | HTML fixture | 2026-08-10 |
| IW-018 | 3 | integrated-worker | crawler | Kakao HTML parser | `KakaoPostParser` | N/A | Todo | N/A | P0 | Todo | HTML fixture | 2026-08-10 |
| IW-019 | 3 | integrated-worker | crawler | Toss/Line/Kakao Selenium crawler | `*BlogCrawler` | N/A | Todo | N/A | P3 | Todo | E2E 최후 | 2026-08-10 |

---

## 8. Phase 4 — interaction-service

**역할:** 검색, 좋아요/북마크/댓글/신고, 조회수 Redis flush, ES count sync.  
**현황:** `src/test` 비어 있음.  
**권장 세로 순서:** postlike → bookmark → comment → report → post → flush/sync.

| ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일 |
|----|-------|------|--------|--------|---------|-----------|---------|-------------------|----------|------|------|--------|
| IS-001 | 4 | interaction-service | postlike | like | `POST /interaction/v1/postings/{postId}/likes` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-002 | 4 | interaction-service | postlike | unlike | `DELETE /interaction/v1/postings/{postId}/likes` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-003 | 4 | interaction-service | postbookmark | bookmark | `POST /interaction/v1/postings/{postId}/bookmarks` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-004 | 4 | interaction-service | postbookmark | unbookmark | `DELETE /interaction/v1/postings/{postId}/bookmarks` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-005 | 4 | interaction-service | postbookmark | my bookmarks | `GET /interaction/v1/me/bookmarks` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-006 | 4 | interaction-service | postcomment | create comment | `POST /interaction/v1/postings/{postId}/comments` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-007 | 4 | interaction-service | postcomment | list comments | `GET /interaction/v1/postings/{postId}/comments` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-008 | 4 | interaction-service | postcomment | update comment | `PATCH /interaction/v1/comments/{commentId}` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-009 | 4 | interaction-service | postcomment | delete comment | `DELETE /interaction/v1/comments/{commentId}` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-010 | 4 | interaction-service | postcomment | create reply | `POST /interaction/v1/comments/{commentId}/replies` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-011 | 4 | interaction-service | postcomment | list replies | `GET /interaction/v1/comments/{commentId}/replies` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-012 | 4 | interaction-service | postcomment | update reply | `PATCH /interaction/v1/replies/{replyId}` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-013 | 4 | interaction-service | postcomment | delete reply | `DELETE /interaction/v1/replies/{replyId}` | Todo | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-014 | 4 | interaction-service | postcomment | my comments | `GET /interaction/v1/me/comments` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-015 | 4 | interaction-service | postreport | create report | `POST /interaction/v1/postings/{postId}/reports` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-016 | 4 | interaction-service | postreport | admin list | `GET /interaction/v1/admin/reports/postings` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-017 | 4 | interaction-service | postreport | admin change status | `PATCH /interaction/v1/admin/reports/postings/{reportId}` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-018 | 4 | interaction-service | commentreport | create report | `POST /interaction/v1/comments/{commentId}/reports` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-019 | 4 | interaction-service | commentreport | admin list | `GET /interaction/v1/admin/reports/comments` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-020 | 4 | interaction-service | commentreport | admin change status | `PATCH /interaction/v1/admin/reports/comments/{reportId}` | Todo | Todo | Todo | P1 | Todo | | 2026-08-10 |
| IS-021 | 4 | interaction-service | post | search postings | `GET /interaction/v1/postings` | Todo | Todo | Todo | P0 | Todo | ES | 2026-08-10 |
| IS-022 | 4 | interaction-service | post | get posting + view | `GET /interaction/v1/postings/{postId}` | Todo | Todo | Todo | P0 | Todo | Redis view | 2026-08-10 |
| IS-023 | 4 | interaction-service | post | view count flush | `PostViewCountFlushWorker` / `PostViewCountFlushService` | N/A | Todo | Todo | P0 | Todo | Redis→ES/DB | 2026-08-10 |
| IS-024 | 4 | interaction-service | post | counts sync | `PostCountsSyncWorker` / `PostCountsSyncService` | N/A | Todo | Todo | P0 | Todo | | 2026-08-10 |
| IS-025 | 4 | interaction-service | user | local user query | `UserService` (shared DB) | Todo | Todo | N/A | P2 | Todo | HTTP 아님 | 2026-08-10 |

---

## 9. Phase 5 — 통합 테스트 (선택, 소수)

단위 테스트로 대체할 수 없는 **배선**만 검증. 클래스명 `*IT`.

| ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일 |
|----|-------|------|--------|--------|---------|-----------|---------|-------------------|----------|------|------|--------|
| US-013 | 5 | user-service | auth | Google OAuth HTTP | `GoogleOAuth*` | N/A | Todo | N/A | P3 | Todo | Phase1과 동일 행, IT/실호출 시 | 2026-08-10 |
| US-014 | 5 | user-service | security | Security filter + JWT | SecurityConfig | N/A | N/A | Todo | P2 | Todo | `@SpringBootTest` 소수 | 2026-08-10 |
| US-015 | 5 | user-service | persistence | Flyway + QueryDSL smoke | repositories | N/A | Todo | N/A | P2 | Todo | Testcontainers Postgres | 2026-08-10 |
| IA-025 | 5 | integrated-api | elasticsearchindex | ES provision/mapping smoke | common-elasticsearch | N/A | Todo | N/A | P2 | Todo | Testcontainers ES | 2026-08-10 |
| IS-026 | 5 | interaction-service | post | Redis view flush round-trip | flush worker | N/A | Todo | Todo | P2 | Todo | | 2026-08-10 |
| IW-019 | 5 | integrated-worker | crawler | Selenium E2E | `*BlogCrawler` | N/A | Todo | N/A | P3 | Todo | Phase3과 동일 후보, 최후 | 2026-08-10 |

---

## 10. 새 기능 추가 시 체크리스트 (복붙용)

- [ ] 단일 스키마 13컬럼으로 행을 추가했는가? (`ID…갱신일`)
- [ ] OpenAPI path 또는 Worker/Scheduler/Util 출처 키가 정확한가?
- [ ] Jacoco로 매핑 클래스 LINE을 확인했는가? (`Done` = ≥80%, 미달이면 `Partial`)
- [ ] Validator / Service / Controller·Worker 값이 `Done|Partial|Todo|N/A`인가?
- [ ] 우선순위·상태·갱신일을 채웠는가?
- [ ] 테스트 메서드명은 한국어인가?
- [ ] 외부 I/O는 mock 경계인가? (실연동은 Phase 5)

---

## 11. 현황 대시보드 (주 1회 갱신 권장)

| Phase | 모듈 | 목표 | 대략 현황 | 다음 액션 |
|-------|------|------|-----------|-----------|
| 0 | common | 헬퍼·컨벤션 | Done (CM-003/005 백로그) | — |
| 1 | user-service | Jacoco LINE ≥80% (Service~) | **모듈 83.9% 통과** | Phase 2 |
| 2 | integrated-api | Validator + 빈 구멍 | In progress (아직 Jacoco 미재계산) | IA-020~024 후 Jacoco |
| 3 | integrated-worker | 파서/크롤러 | In progress (아직 Jacoco 미재계산) | IW-015~018 |
| 4 | interaction-service | 전 도메인 신규 | Todo | IS-001부터 |
| 5 | IT | 배선 소수 | Todo | 필요 시 |

마지막 갱신일: `2026-08-10` (Jacoco 범위: Repository 제외, Service부터. user-service 83.9%)

---

## 12. 참고 링크

| 문서 | 용도 |
|------|------|
| [docs/api/README.md](./api/README.md) | OpenAPI 스냅샷·갱신 |
| [docs/custom-exception-guide.md](./custom-exception-guide.md) | 예외/에러코드 |
| [docs/elasticsearch-index-management.md](./elasticsearch-index-management.md) | ES 인덱스 운영 |
| 루트 `README.md` | `./gradlew test` 실행 |

관련 코드:

- `common-data-access/src/testFixtures/.../MockJwtPrincipalResolver.java`
- 예시 Service 테스트: `user-service/.../UserServiceTest.java`
- 예시 Controller 테스트: `user-service/.../UserControllerTest.java`
- 예시 Worker 테스트: `integrated-worker/.../IndexingJobWorkerTest.java`
