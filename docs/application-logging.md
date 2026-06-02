# 애플리케이션 로깅 (`@Slf4j`)

코드에서 **어디에**, **어떤 레벨로**, **어떤 형식으로** 로그를 남길지 정의합니다.

인프라(Logback·파일·롤링)는 [infrastructure-logging.md](./infrastructure-logging.md)를 참고하세요.

| 구분 | 문서 |
|------|------|
| Logback, yml, 롤링, `[integrated-api]` 접두 (서비스명) | [infrastructure-logging.md](./infrastructure-logging.md) |
| `@Slf4j`, 도메인·`ErrorCode`, 메시지 본문 | **이 문서** |

---

## 1. 목표

- API 에러 응답의 `code`(`ErrorCode`)와 **로그에서 동일 문자열로 검색** 가능
- Worker / 외부 연동 / 인프라 실패는 운영 추적 가능, CRUD Controller는 노이즈 최소
- 메시지·레벨·`@Slf4j` 위치를 팀 전체가 동일 규칙으로 유지

---

## 2. `ErrorCode` 와 API 응답

`common-data-access`의 `ErrorCode`는 HTTP API 에러 JSON의 `code` 필드와 같습니다.

```java
// ErrorCode 예
BE_NOT_FOUND(404, "BE40401", "리소스를 찾을 수 없습니다."),
FE_INVALID_INPUT_VALUE(400, "FE40001", "입력 데이터에 문제가 있습니다."),
IE_ELASTICSEARCH_ERROR(500, "IE50001", "서버 처리 오류(관리자에게 문의하세요)"),
```

| 접두 | 의미 | 로그에 쓸 때 |
|------|------|----------------|
| `BE` | Backend 비즈니스 예외 | `BusinessException` throw·catch 시 |
| `FE` | Framework (validation, 405 등) | `GlobalExceptionHandler` 등 |
| `IE` | Infra (ES, Redis, DB, OAuth) | `InfraException`, ES translator |

클라이언트가 `BE40401`을 받으면, 서버 로그에서 **`[BE40401]`** 로 grep 하는 것을 표준으로 합니다.

---

## 3. 메시지 형식

### 3.1 기본 구조

```text
[도메인] 설명 key1={} key2={}
```

- **도메인**: 기능 단위 영문 PascalCase — `[CollectingJob]`, `[IndexingJob]`, `[PostLike]`, `[ES]`, `[Auth]`
- **설명**: 짧은 한국어 또는 영문 (팀 내 하나로 통일 권장)
- **파라미터**: `key={}` 만 사용, **엔티티/`toString()` 전체 금지**

### 3.2 `ErrorCode` 포함 (API·예외와 연관될 때)

```text
[도메인][ErrorCode.code] 설명 key1={} key2={}
```

- `ErrorCode.code` 값을 **대괄호 그대로** — `[BE40401]`, `[IE50001]`
- API 응답 `code` 필드와 **동일 문자열**

```java
log.warn("[CollectingJob][BE40901] crawler conflict sourceId={}", sourceId);

BusinessException e = ...;
log.debug("[Post][BE40401] post not found postId={}", postId, e);

log.error("[IndexingJob][BE50001] job failed jobId={}", jobId, e);
```

### 3.3 `ErrorCode` 를 붙이지 않는 경우

| 상황 | 예 |
|------|-----|
| 정상 처리·배치 요약 | `[CollectingJob] job finished jobId={} processed={}` |
| 복구 가능·idempotent (API code 없음) | `[PostLike] duplicate like, idempotent userId={} postId={}` |
| 디버그·내부 상태 | `[CollectingJob] content unchanged postId={}` |

### 3.4 출력 예 (인프라 패턴 + 애플리케이션 본문)

```text
[integrated-api] WARN  2026-06-02 17:30:00.123 [http-nio-8081-exec-3] c.b.i.c.s.CollectingJobService - [CollectingJob][BE40401] collecting job not found jobId=550e8400-e29b-41d4-a716-446655440000
```

앞의 `[integrated-api]`는 Logback `APP_NAME`, 뒤 `[CollectingJob][BE40401]`부터가 `%msg`입니다.

### 3.5 금지

- password, authorization code, JWT, refresh token
- email 전문, OAuth `subject` / id_token 일부
- `log.info("post: {}", post)` 처럼 **객체 전체 dump**
- 같은 실패에 **stack trace 이중** (한 곳에서만 `e` 마지막 인자)

---

## 4. `@Slf4j` 를 붙이는 클래스

### 붙인다

| 계층 | 예 |
|------|-----|
| Worker / Scheduler | `CollectingJobWorker`, `PostCountsSyncWorker` |
| Executor (비동기·job) | `CollectingJobExecutor`, `IndexingJobExecutor` |
| 외부 OAuth | `GoogleOAuthTokenClient`, `GoogleIdTokenVerifierService` |
| 인프라 Repository 구현 | ES/Redis custom repository |
| common 횡단 | `GlobalExceptionHandler`, `JwtAuthenticationFilter`, `ElasticsearchExceptionTranslator` |

### 붙이지 않는다

| 계층 | 예 |
|------|-----|
| DTO, Validator, enum | |
| thin Controller (Service 위임만) | 대부분의 `*Controller` |
| thin Service (CRUD + `BusinessException`만) | |
| QueryDSL QueryRepository | |
| `BlogCrawlerService`, Crawler Parser 하위 | 실패 시 throw → `CollectingJobExecutor.markFailed`에서 로그 |

**원칙:** 운영·장애 분석에 직접 쓰일 로그를 남기는 클래스만 `@Slf4j`.

---

## 5. 로그 레벨

`GlobalExceptionHandler`(`common-web`)와 맞춥니다.

| 상황 | 레벨 | `ErrorCode` in msg | 로그 위치 |
|------|------|-------------------|-----------|
| `BusinessException` → API 응답 (예상 거부) | **없음** 또는 `debug` | 있으면 `[BE40401]` 등 | throw 지점 또는 handler (한 곳) |
| 4xx 성격 입력 오류 | `debug` | `[FE40001]` 등 | handler |
| 복구 가능 (idempotent, skip, not-found 조회) | `debug` / `warn` | 보통 없음 | 발생 지점 1곳 |
| 배치·job **전체 실패** | `error` + `e` | `[BE50001]` / `[IE50001]` 등 | Executor / Worker |
| 배치 **부분 실패** | `warn` + 집계 | 선택 | Worker |
| 정상 배치 요약 (건수 > 0) | `debug` | 없음 | Worker |
| 미처리 예외 | `error` + `e` | `[FE50001]` | handler |
| 스케줄 heartbeat (`call poll()`) | **금지** (`info` 매 tick) | — | |

### `BusinessException` 과 handler

- Service는 **throw만**, `GlobalExceptionHandler`는 **로그 없음** (응답 body에 `code` 있음)
- 미처리 예외만 handler에서 `[Framework][FE50001]` 로 `error` 기록
- Service에서 `warn` + handler 응답 → **중복 금지**

### 레벨 통일 예: ES 문서 없음

| 맥락 | 레벨 |
|------|------|
| 선택 조회 (없어도 정상) | `debug` |
| 동기화·필수 문서 없음 | `warn` |

서비스·모듈 내에서 한 가지로 맞출 것.

---

## 6. 계층별 가이드

### Worker / Scheduler

```java
// X: 매 tick
log.info("CollectingJob Worker call poll()");

// O: job이 있을 때만
log.debug("[CollectingJob] jobs picked count={}", pickedJobIds.size());

// O: 실패
log.error("[CollectingJob][BE50001] poll failed", e);

// O: 부분 실패 (PostCountsSyncWorker 스타일)
log.warn("[PostCount] ES sync partial failure totalPosts={} failedCount={}", total, failed);
```

### Executor

```java
catch (Exception e) {
    markFailed(jobId, e);  // 내부에서 [CollectingJob][ErrorCode] error 1줄
}
```

- 루프 **매 아이템 `info`** 지양 → `debug` 또는 job 종료 시 건수만
- `markFailed` / catch 경계에 **반드시 `error` 1줄**

### Collecting job · 크롤 (`BlogCrawlerService`)

- **한 페이지라도 크롤 실패 → 전체 CollectingJob 실패** (`CrawlingException` rethrow → `CollectingJobExecutor.markFailed`)
- API 응답 code: `BE40902` (`ErrorCode.BE_CRAWLER_CONFLICT`)
- 크롤 구간에서는 **로그를 남기지 않고** throw만 한다. job 단위 `error` 로그는 `markFailed` 한 곳 (중복·이중 stack 방지)
- 부분 성공(일부 페이지만 수집 후 SUCCESS)은 **허용하지 않음**

### Service (API)

- 기본: 로그 없음 (`BusinessException` → API `code`)
- 로그 시: 외부 호출 실패, catch 후 복구 — `ErrorCode` 알 때 `[도메인][IE50001]`

### Controller

- 기본: **무로그** (`AuthController` 포함)
- OAuth 실패 추적은 **`GoogleOAuthTokenClient` / `GoogleIdTokenVerifierService`** 에서 `[Auth][IE50004]` 등으로 기록

### 외부 OAuth (user-service)

```java
log.error("[Auth][IE50004] Google token exchange failed", e);
log.warn("[Auth][BE40101] Google id_token verification failed");
```

- authorization code, id_token 본문, `subject` / email **로그 금지**

### Infra (ES / Redis / common)

`ElasticsearchExceptionTranslator` 예:

```java
log.error("[ES][IE50001] {} failed status={} reason={}", operation.name(), status, reason, e);
```

---

## 7. `ErrorCode` 매핑 참고

로그에 code를 붙일 때는 **실제 throw·응답에 쓰는 code**와 동일하게.

| 예외 / 상황 | code |
|-------------|------|
| 리소스 없음 | `BE40401` |
| 상태 충돌 | `BE40901` |
| 크롤링 데이터 오류 | `BE40902` |
| ES 장애 | `IE50001` |
| Redis 장애 | `IE50002` |
| 미처리 서버 오류 (handler) | `FE50001` |
| Google OAuth / id_token | `IE50004` |

전체 목록: `common-data-access/.../ErrorCode.java`

---

## 8. 체크리스트 (리뷰용)

- [ ] `@Slf4j`가 필요한 클래스에만 있는가
- [ ] API 응답 `code`와 연관된 로그에 `[BE40401]` 형태가 있는가
- [ ] 정상 흐름에 불필요한 `ErrorCode` 접두가 없는가
- [ ] `error`에 예외 `e`가 마지막 인자인가
- [ ] heartbeat / 객체 dump / 민감 정보가 없는가
- [ ] `BusinessException` 이중 로그가 없는가

---

## 9. 관련 파일

| 경로 |
|------|
| `common-data-access/.../exception/ErrorCode.java` |
| `common-data-access/.../exception/BusinessException.java` |
| `common-web/.../GlobalExceptionHandler.java` |
| `common-elasticsearch/.../ElasticsearchExceptionTranslator.java` |
| `user-service/.../oauth/google/GoogleOAuthTokenClient.java` |
| `user-service/.../oauth/google/GoogleIdTokenVerifierService.java` |
| [infrastructure-logging.md](./infrastructure-logging.md) |
