# Custom Exception 아키텍처 가이드

블로그 수집기 시스템(Blog Collector System)의 여러 모듈(`common-data-access`, `common-elasticsearch`, `common-logging`, `common-web`, `integrated-api`, `integrated-worker`, `interaction-service`, `user-service`)에 걸쳐 일관되게 적용된 **Custom Exception 처리 체계**를 정리한 문서입니다.

---

## 1. 전체 구조 (계층 흐름)

```
애플리케이션 코드 (Validator / Service / Domain Entity)
        │  throw BadRequestException / NotFoundException / ...
        ▼
BusinessException (abstract, common-data-access)   ← ErrorCode + errorMessage 보유
        │
        ├─▶ [API 모듈] GlobalExceptionHandler (@RestControllerAdvice, common-web)
        │           └─▶ ErrorResponse JSON { code, message, status, timestamp }
        │
        └─▶ [Worker 모듈] Executor/Worker 가 catch → 로그 + markFailed()
```

| 계층 | 모듈 | 역할 |
|------|------|------|
| 예외 정의 + ErrorCode | `common-data-access` | 모든 모듈이 의존하는 기반 타입 |
| HTTP 에러 처리 | `common-web` | `GlobalExceptionHandler`, `ErrorResponse`, 시큐리티 진입점 |
| 인프라 예외 변환 | `common-elasticsearch` | `ElasticsearchExceptionTranslator` 가 ES 오류를 `BusinessException` 으로 래핑 |
| 로깅 설정 | `common-logging` | 예외 관련 Java 코드 없음 (설정 전용) |

---

## 2. 핵심 뼈대: BusinessException + ErrorCode

모든 커스텀 예외의 부모는 `RuntimeException`을 상속한 추상 클래스 하나로 통일했습니다.

```java
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String errorMessage;
    // 4개의 생성자: 기본 메시지 / 커스텀 메시지 / 메시지+원인 / 원인만
}
```

- 모든 예외가 `ErrorCode`를 반드시 갖도록 강제 → HTTP 응답 코드와 메시지가 일관됨
- 원인(cause)을 함께 전달하는 생성자를 제공하여 스택 트레이스 보존

`ErrorCode`는 **접두어로 발생 계층을 구분**하는 규칙을 세웠습니다.

| 접두어 | 의미 | 예시 |
|--------|------|------|
| **BE** | Backend (비즈니스/도메인) | `BE40401` (NotFound), `BE40901` (Conflict) |
| **FE** | Framework (Spring MVC 등) | `FE40001`, `FE50001` |
| **IE** | Infra (ES / Redis / PG / Google) | `IE50001` ~ `IE50004` |

---

## 3. 상황별 전용 예외 클래스 (총 7종)

`BusinessException`을 상속한 구체 예외를 상황별로 나눠, 코드에서 의미가 바로 드러나도록 했습니다.

| 예외 클래스 | HTTP | 매핑 ErrorCode | 사용 상황 |
|-------------|------|----------------|-----------|
| `BadRequestException` | 400 | `BE_INVALID_INPUT_VALUE` | 입력값 검증 실패 |
| `UnauthorizedException` | 401 | `BE_UNAUTHORIZED` | 인증 실패 (JWT, 비밀번호 등) |
| `AccessDeniedException` | 403 | `BE_FORBIDDEN` | 권한 없음 |
| `NotFoundException` | 404 | `BE_NOT_FOUND` | 리소스 조회 실패 |
| `StateConflictException` | 409 | `BE_CONFLICT` | 상태 충돌 (중복, 잘못된 상태 전이) |
| `CrawlingException` | 409 | `BE_CRAWLER_CONFLICT` | 크롤링 대상 데이터 이상 |
| `InfraException` | 500 | `IE_*` / `BE_INTERNAL_ERROR` | 외부 인프라 연동 실패 |

---

## 4. 일관성을 지킨 4가지 규칙

### 규칙 ① 검증은 `*Validator`에 모으고 전용 예외로 던진다
- 각 도메인마다 `CollectingJobValidator`, `UserValidator`, `PostCommentValidator` 등 전용 검증 클래스
- 입력값 → `BadRequestException`, 조회 실패 → `NotFoundException`

### 규칙 ② 엔티티 조회는 `orElseThrow`로 통일

```java
.orElseThrow(() -> new NotFoundException("존재하지 않는 collectJob입니다. id: " + id));
```

### 규칙 ③ 도메인 상태 전이 규칙은 Entity 내부에서 `StateConflictException`으로 방어

```java
throw new StateConflictException("PENDING이 아닌 Job은 RUNNING으로 못 바꿈");
```

### 규칙 ④ 인프라 예외는 삼키지 않고 `InfraException`으로 감싸 원인(cause) 보존
- Google OAuth, Elasticsearch(`ElasticsearchExceptionTranslator`), 해시 알고리즘 등 체크 예외를 잡아 `InfraException(ErrorCode.IE_*, message, e)`로 변환

---

## 5. 모듈별 처리 방식의 차이 (의도된 설계)

- **API 모듈** (`integrated-api`, `interaction-service`, `user-service`): 예외가 `common-web`의 `GlobalExceptionHandler`까지 전파 → 통일된 JSON 응답
- **Worker 모듈** (`integrated-worker`): `common-web`에 의존하지 않고, Executor/Worker 최상단에서 catch → 에러코드 로깅 + 작업 실패 상태 저장 (스케줄러는 계속 진행)

---

## 6. 마지막으로 확립한 원칙: raw 표준 예외를 흘려보내지 않는다

크롤러 파서에서 `Objects.requireNonNull()`로 인해 메시지 없는 `NullPointerException`이 나던 부분을, 모두 명시적 null 체크 + `CrawlingException`으로 교체했습니다.

```java
String href = linkElement.getAttribute("href");
if (href == null) {
    throw new CrawlingException("KakaoPost href attribute not found");
}
String url = href.startsWith("http") ? href : "https://tech.kakao.com" + href;
```

이로써 **프로덕션 코드 전체에서 raw `RuntimeException` / `IllegalArgumentException` / NPE를 직접 던지는 곳이 사라졌고**, 모든 예외가 `BusinessException` 체계를 거치게 되었습니다.

---

## 한 줄 요약

> 추상 `BusinessException` + 계층 구분 `ErrorCode`(BE/FE/IE)를 뿌리로 두고, 상황별 7종 전용 예외를 Validator·Entity·Infra 계층에서 일관되게 던지며, API는 `GlobalExceptionHandler`로 통일 응답 / Worker는 로깅 후 실패 처리하는 구조.
