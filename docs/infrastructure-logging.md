# 인프라 로깅 (Logback · yml)

`common-logging` 모듈을 통해 **4개 Boot 서비스**에 동일하게 적용되는 **인프라 레벨** 설정입니다.

| 적용 서비스 | `spring.application.name` |
|-------------|----------------------------|
| integrated-api | `integrated-api` |
| integrated-worker | `integrated-worker` |
| interaction-service | `interaction-service` |
| user-service | `user-service` |

**범위**

- 포함: Logback appenders, **한 줄 출력 패턴**(타임스탬프·레벨·logger), 파일 경로, 롤링·보관·용량, dev/prod 파일 출력
- 미포함: `@Slf4j`, 메시지 문구, `ErrorCode` — [application-logging.md](./application-logging.md)

---

## 1. 구조 개요

```
common-logging/
  src/main/resources/
    logback-spring.xml
    application-logging-defaults.yml

각 Boot 서비스/
  build.gradle                      # implementation project(":common-logging")
  application.yml                   # spring.config.import
```

| 파일 | 역할 |
|------|------|
| `application-logging-defaults.yml` | 레벨·경로·롤링·파일 on/off **값** |
| `logback-spring.xml` | Appender 구조 + yml `springProperty` 연결 |
| 서비스 `application.yml` | `spring.application.name`, import, override |

### 왜 yml 중심인가

xml에 숫자·경로를 하드코딩하면 운영 yml 변경만으로는 반영되지 않고 **JAR 재빌드**가 필요합니다.  
롤링·경로·appender on/off는 `logging.*` / `logging.backend.*`로 두고, xml은 연결만 담당합니다.

**주의:** 설정 변경 후 **재기동** 필요. JAR 없이 바꾸려면 ConfigMap·환경변수 등 **런타임 주입** 사용.

---

## 2. 서비스 연결

```gradle
implementation project(":common-logging")
```

```yaml
spring:
  application:
    name: <서비스명>
  config:
    import: optional:classpath:application-logging-defaults.yml
```

서비스 `resources/logback-spring.xml`은 **두지 않음**.

---

## 3. Logback 출력 패턴 (인프라)

콘솔·파일·ERROR 파일 공통 (`LOG_PATTERN`):

```text
[${APP_NAME}] %-5level %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %logger{36} - %msg%n
```

| 요소 | 예 |
|------|-----|
| `[${APP_NAME}]` | `[integrated-api]` |
| `%msg` | 애플리케이션 `log.*("...")` 본문 — [application-logging.md](./application-logging.md) 형식 권장 |

```text
[integrated-worker] INFO  2026-06-02 17:30:00.123 [scheduling-1] c.b.i.c.s.CollectingJobWorker - [CollectingJob] job finished jobId={}
```

`%msg` 안의 `[도메인][BE40401]` 은 **애플리케이션 정책**에서 붙입니다.

---

## 4. Appender · 프로파일

| Appender | 활성 조건 |
|----------|-----------|
| `consoleAppender` | 항상 |
| `fileAppender` + `errorAppender` | `logging.backend.file-appender-enabled=true` |

- 기본 / local: `false` → 콘솔만  
- `dev`, `prod` (defaults 2번째 yaml 문서): `true` → 콘솔 + 파일 + ERROR 전용 파일

---

## 5. 로그 파일 경로

```text
{logging.file.path}/{logging.file.name}/
  {name}.log
  {name}.2026-06-02.0.log.gz
  error/error.log
```

기본: `logging.file.path=./logs`, `logging.file.name=${spring.application.name}`

---

## 6. 롤링·보관 (yml)

| yml 키 | 기본값 |
|--------|--------|
| `logging.logback.rollingpolicy.max-file-size` | `100MB` |
| `logging.logback.rollingpolicy.max-history` | `90` (일) |
| `logging.logback.rollingpolicy.total-size-cap` | `10GB` |
| `logging.backend.rolling.file-name-pattern` | `.%d{yyyy-MM-dd}.%i.log.gz` |
| `logging.backend.error.total-size-cap` | `3GB` |

---

## 7. 레벨 기본값 (yml)

| yml 키 | 기본값 |
|--------|--------|
| `logging.level.root` | `INFO` |
| `logging.level.com.backend` | `INFO` |
| `logging.level.org.springframework` | `WARN` |
| `logging.level.org.hibernate.SQL` | `WARN` |

---

## 8. 설정 변경 · 로컬 실행

**팀 공통:** `application-logging-defaults.yml`  
**환경만:** 서비스 `application-prod.yml` 또는 `LOGGING_*` 환경변수  
**구조 변경:** `logback-spring.xml` + 재빌드

```bash
./gradlew :integrated-api:bootRun
./gradlew :integrated-api:bootRun --args='--spring.profiles.active=prod'
```

---

## 9. xml ↔ yml 매핑

| yml | 용도 |
|-----|------|
| `logging.file.path` / `name` | 파일 경로 |
| `logging.backend.file-appender-enabled` | 파일·error on/off |
| `logging.logback.rollingpolicy.*` | 롤링 |
| `logging.backend.rolling.file-name-pattern` | 압축·롤링 파일명 접미사 |
| `logging.backend.error.*` | ERROR 디렉터리·용량 |

---

## 10. 관련 문서 · 파일

| 문서 | 내용 |
|------|------|
| [application-logging.md](./application-logging.md) | `@Slf4j`, 메시지, `ErrorCode`, 레벨 규칙 |

| 경로 |
|------|
| `common-logging/src/main/resources/logback-spring.xml` |
| `common-logging/src/main/resources/application-logging-defaults.yml` |
