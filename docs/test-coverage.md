# 테스트 커버리지 현황

대상 모듈: `user-service`, `integrated-api`, `integrated-worker`, `interaction-service`.

**80%를 확인하는 방법 (두 단계):**

| 질문 | 어디서 보나 | 판정 |
|------|-------------|------|
| 이 모듈 전체가 LINE 80%인가? | 아래 **모듈 게이트** 표, 또는 `./scripts/check-coverage.sh` | Gradle `jacocoTestCoverageVerification` |
| 이번 기능의 클래스가 LINE 80%인가? | 각 모듈 **80% 미만 클래스** 표 | 그 표에 없으면, 측정된 클래스는 모두 ≥ 80% |
| API·워커가 빠졌는가? | 각 모듈 **기능 체크리스트** | Jacoco는 엔드포인트 누락을 알려주지 않음 |

모듈 합산이 80%여도, 작은 클래스는 미달일 수 있다. (예: user-service 모듈은 통과, `GoogleOAuthTokenClient`는 4.5%.)  
신규 기능은 **스크립트로 모듈 게이트**를 확인하고, **미달 클래스 표**로 해당 기능을 확인한다.

체크리스트 컬럼 (생략 금지):

`ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일`

| 컬럼 | 허용 값 |
|------|---------|
| Validator / Service / Controller/Worker | `Done` · `Partial` · `Todo` · `N/A` |
| 우선순위 | `P0` · `P1` · `P2` · `P3` |
| 상태 | `Todo` · `In progress` · `Done` · `Blocked` |

### Done 판정 (클래스 LINE ≥ 80%)

측정 범위: Controller / Validator / Service / Worker·Runner / Util.  
제외: Repository(`**/repository/**`), DTO, config, `*Application`, QueryDSL `Q*`.

| 레이어 값 | 의미 |
|-----------|------|
| `Done` | 매핑 클래스 LINE ≥ 80% |
| `Partial` | 테스트는 있으나 LINE **&lt; 80%** |
| `Todo` | 유의미한 테스트 없음 |
| `N/A` | 해당 레이어 없음 |

행 `상태=Done`: 필요한 레이어가 모두 `Done` 또는 `N/A`. 테스트 파일만 있으면 `Partial`.

### 측정 명령

```bash
./scripts/check-coverage.sh                 # 4개 모듈 게이트
./scripts/check-coverage.sh user-service    # 한 모듈만

./gradlew :user-service:test :user-service:jacocoTestReport
./gradlew :user-service:jacocoTestCoverageVerification
# HTML: {module}/build/reports/jacoco/test/html/index.html
```

ID 접두어: `US` · `IA` · `IW` · `IS`. Phase는 ID에 넣지 않고 컬럼으로 둔다.

---

## 모듈 게이트 (한눈에)

실시간 판정은 스크립트. 이 표는 마지막 측정 스냅샷이다.

| 모듈 | LINE | 판정 | 갱신일 |
|------|------|------|--------|
| user-service | 83.9% (312/372) | 통과 | 2026-08-10 |
| integrated-api | 미측정 | — | — |
| integrated-worker | 미측정 | — | — |
| interaction-service | 미측정 (`src/test` 비어 있음) | — | — |

---

## 진행 순서

```text
1. user-service
   ↓
2. integrated-api
   ↓
3. integrated-worker
   ↓
4. interaction-service
```

모듈 안 테스트 작성 순서: `*ValidatorTest` → `*ServiceTest` / `*ExecutorTest` / `*PickerTest` / `*WorkerTest` → `*ControllerTest`.

---

## 1. user-service

JWT 발급, 회원가입/계정 병합, admin bootstrap. `common-data-access`, `common-web`.

| 범위 | LINE | 판정 | 갱신일 |
|------|------|------|--------|
| 모듈 (Repository 제외) | 83.9% (312/372) | 통과 | 2026-08-10 |

80% 미만 클래스 (나머지는 ≥ 80%):

| 클래스 | LINE | 대응 행 |
|--------|------|---------|
| `GoogleOAuthTokenClient` | 4.5% | US-013 |
| `GoogleIdTokenVerifierService` | 3.1% | US-013 |

| ID | Phase | 모듈 | 도메인 | 기능명 | 출처 키 | Validator | Service | Controller/Worker | 우선순위 | 상태 | 비고 | 갱신일 |
|----|-------|------|--------|--------|---------|-----------|---------|-------------------|----------|------|------|--------|
| US-001 | 1 | user-service | auth | password login | `POST /auth/v1/auth/login` | Done | Done | Done | P0 | Done | | 2026-08-10 |
| US-002 | 1 | user-service | auth | Google OAuth callback | `GET /auth/v1/oauth/google/callback` | Done | Done | Done | P0 | Done | Google HTTP는 US-013 | 2026-08-10 |
| US-003 | 1 | user-service | user | signup | `POST /user/v1/users` | Done | Done | Done | P0 | Done | | 2026-08-10 |
| US-004 | 1 | user-service | user | get me | `GET /user/v1/users/me` | Done | Done | Done | P0 | Done | | 2026-08-10 |
| US-005 | 1 | user-service | user | update me | `PATCH /user/v1/users/me` | Done | Done | Done | P0 | Done | | 2026-08-10 |
| US-006 | 1 | user-service | user | change password | `PATCH /user/v1/users/me/password` | Done | Done | Done | P0 | Done | | 2026-08-10 |
| US-007 | 1 | user-service | user | merge oauth | `POST /user/v1/users/me/merge-oauth` | Done | Done | Done | P0 | Done | | 2026-08-10 |
| US-008 | 1 | user-service | user | delete me | `DELETE /user/v1/users/me` | Done | Done | Done | P0 | Done | | 2026-08-10 |
| US-009 | 1 | user-service | userauthentication | create/read/update/merge/delete | `UserAuthenticationService` | Done | Done | N/A | P0 | Done | | 2026-08-10 |
| US-010 | 1 | user-service | bootstrap | admin bootstrap | `AdminBootstrapRunner` | N/A | Done | Done | P0 | Done | | 2026-08-10 |
| US-011 | 1 | user-service | utils | random nickname | `NicknameGenerateUtil` | N/A | Done | N/A | P1 | Done | | 2026-08-10 |
| US-012 | 1 | user-service | auth | JWT issue | `JwtAuthenticationTokenIssuer` | N/A | Done | N/A | P0 | Done | | 2026-08-10 |
| US-013 | 1 | user-service | auth | Google OAuth HTTP clients | `GoogleOAuthTokenClient` / `GoogleIdTokenVerifierService` | N/A | Todo | N/A | P3 | Todo | mock 유지 또는 이후 IT | 2026-08-10 |

---

## 2. integrated-api

관리자용 provider / collect source / job / indexed post / ES index. shared DB + ES.

| 범위 | LINE | 판정 | 갱신일 |
|------|------|------|--------|
| 모듈 (Repository 제외) | 미측정 | — | — |

80% 미만 클래스: 미측정. `./scripts/check-coverage.sh integrated-api` 후 LINE &lt; 80%인 클래스만 이 표에 남긴다.

아래 기능 행의 Done/Partial은 **Jacoco 재측정 전 추정**. 측정하면 레이어 칸을 고친다.

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
| IA-020 | 2 | integrated-api | indexedpost | get indexed posting | `GET /index/v1/postings/{posting-id}` | Todo | Todo | Todo | P0 | Todo | 빈 구멍 | 2026-08-10 |
| IA-021 | 2 | integrated-api | elasticsearchindex | bootstrap | `POST /index/v1/elasticsearch/_bootstrap` | N/A | Todo | Todo | P0 | Todo | 빈 구멍 | 2026-08-10 |
| IA-022 | 2 | integrated-api | elasticsearchindex | reindex alias | `POST /index/v1/elasticsearch/_reindex` | N/A | Todo | Todo | P0 | Todo | 빈 구멍 | 2026-08-10 |
| IA-023 | 2 | integrated-api | elasticsearchindex | status | `GET /index/v1/elasticsearch/status` | N/A | Todo | Todo | P0 | Todo | 빈 구멍 | 2026-08-10 |
| IA-024 | 2 | integrated-api | elasticsearchindex | auto bootstrap runner | `ElasticsearchAutoBootstrapRunner` | N/A | Todo | Todo | P1 | Todo | Controller/Worker=Runner | 2026-08-10 |

---

## 3. integrated-worker

Collecting/Indexing Job 폴링, 크롤, Post 적재, ES 색인, stale 복구. shared DB + ES + Selenium.  
REST Controller 없음 → Controller/Worker 칸에 Worker/Picker/Executor를 적는다.

| 범위 | LINE | 판정 | 갱신일 |
|------|------|------|--------|
| 모듈 (Repository 제외) | 미측정 | — | — |

80% 미만 클래스: 미측정. `./scripts/check-coverage.sh integrated-worker` 후 LINE &lt; 80%인 클래스만 이 표에 남긴다.

아래 기능 행의 Done/Partial은 **Jacoco 재측정 전 추정**.

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
| IW-015 | 3 | integrated-worker | crawler | BlogCrawlerService + strategy | `BlogCrawlerService` | N/A | Todo | N/A | P0 | Todo | 빈 구멍 | 2026-08-10 |
| IW-016 | 3 | integrated-worker | crawler | Toss HTML parser | `TossPostParser` | N/A | Todo | N/A | P0 | Todo | HTML fixture | 2026-08-10 |
| IW-017 | 3 | integrated-worker | crawler | Line HTML parser | `LinePostParser` | N/A | Todo | N/A | P0 | Todo | HTML fixture | 2026-08-10 |
| IW-018 | 3 | integrated-worker | crawler | Kakao HTML parser | `KakaoPostParser` | N/A | Todo | N/A | P0 | Todo | HTML fixture | 2026-08-10 |
| IW-019 | 3 | integrated-worker | crawler | Toss/Line/Kakao Selenium crawler | `*BlogCrawler` | N/A | Todo | N/A | P3 | Todo | E2E 최후 | 2026-08-10 |

---

## 4. interaction-service

검색, 좋아요/북마크/댓글/신고, 조회수 Redis flush, ES count sync.  
권장 작성 순서: postlike → bookmark → comment → report → post → flush/sync.

| 범위 | LINE | 판정 | 갱신일 |
|------|------|------|--------|
| 모듈 (Repository 제외) | 미측정 (`src/test` 비어 있음) | — | — |

80% 미만 클래스: 미측정. 테스트가 없으면 측정 대상 클래스가 전부 이 표에 해당한다.

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

## 부록. 작성 규칙

스타일: JUnit 5 + Mockito + AssertJ. `@SpringBootTest` / `@WebMvcTest`는 쓰지 않는다.  
테스트는 프로덕션과 같은 패키지, 이름은 `{ClassName}Test`. `@Nested` + 테스트 메서드명은 한국어 (`~할_수_있다`).

Controller:

```java
mockMvc = MockMvcBuilders.standaloneSetup(controller)
    .setCustomArgumentResolvers(new MockJwtPrincipalResolver())
    .build();
```

기능 행 추가 때 대조할 곳: [`docs/api/`](./api/README.md), `*Service`/`*Validator`/`*Controller`/`*Worker`/`*Runner`/`*Executor`/`*Picker`, API에 안 보이는 스케줄·부트스트랩·외부 클라이언트.

- OpenAPI 기능 → Validator(없으면 N/A) + Service + Controller
- Worker/Scheduler/Runner → Service(또는 동등) + Worker/Runner
- Util → 해당 util만 (Controller `N/A`)

헬퍼: `MockJwtPrincipalResolver`는 `common-data-access` testFixtures (`user()` / `admin()`).  
사용: `user-service`, `integrated-api`, `interaction-service`. worker는 불필요.  
보류: 엔티티 공용 빌더, `@CurrentUser(required=false)` anonymous mock.

결합(테스트 설계): user-service가 JWT 발급, 다른 API는 같은 `jwt.*`로 검증. 수집 Job은 api 기록 → worker 폴링. 검색은 worker ES 색인 → interaction/api 조회. interaction의 User는 공유 PostgreSQL (HTTP 아님).
