# 공통 로깅 정책

이 문서는 `common-logging` 모듈을 통해 **4개 Boot 서비스**에 동일하게 적용되는 **인프라 레벨** 로깅 설정을 설명합니다.

| 적용 서비스 | `spring.application.name` |
|-------------|----------------------------|
| integrated-api | `integrated-api` |
| integrated-worker | `integrated-worker` |
| interaction-service | `interaction-service` |
| user-service | `user-service` |

**범위**

- 포함: 로그 출력 형식(패턴), 레벨 기본값, 파일 저장 경로, 롤링·보관·용량, dev/prod 파일 출력 여부
- 미포함: `@Slf4j`로 **어디에 무엇을 남길지**(애플리케이션 로깅) — 별도 코드/리뷰 정책

---

## 1. 구조 개요

```
common-logging/
  src/main/resources/
    logback-spring.xml              # Appender·패턴·yml 값 연결 (JAR에 포함)
    application-logging-defaults.yml # 팀 공통 기본값 (JAR에 포함)

각 Boot 서비스/
  build.gradle                      # implementation project(":common-logging")
  application.yml                   # spring.config.import 로 defaults 로드
```

### 역할 분담

| 파일 | 역할 |
|------|------|
| `application-logging-defaults.yml` | **값** 일괄 관리 (레벨, 경로, 롤링, 파일 on/off). 배포 시 여기 또는 서비스 yml·환경변수만 바꾸는 것을 목표로 함 |
| `logback-spring.xml` | Appender **구조** 정의 + yml/properties를 `springProperty`로 읽어 연결 |
| 서비스 `application.yml` | `spring.application.name`, `spring.config.import`, 서비스별 override |

### 왜 yml 중심인가

`logback-spring.xml`에 숫자·경로를 **하드코딩**하면, 운영에서 `application.yml`만 바꿔도 Logback에 반영되지 않습니다. 그 경우 `common-logging` 수정 → **전 서비스 JAR 재빌드·CI/CD**가 필요합니다.

지금은 롤링 크기·보관 일수·파일 경로·파일 appender on/off 등을 **Spring Environment(`logging.*`)** 로 두고, xml은 “그 값을 읽어 쓰는 연결”만 담당합니다.

**주의**

- 설정 변경 후에는 보통 **애플리케이션 재기동**이 필요합니다 (Logback은 기동 시 로드).
- repo 안의 yml만 수정해 배포하면 CI는 여전히 돕니다. **JAR 재빌드 없이** 바꾸려면 ConfigMap·환경변수·외부 `application-prod.yml` 등 **런타임 주입**과 함께 써야 합니다.

---

## 2. 서비스 연결 방법

### Gradle

```gradle
dependencies {
    implementation project(":common-logging")
}
```

### application.yml (4개 서비스 공통)

```yaml
spring:
  application:
    name: <서비스명>   # 로그에 [서비스명] 및 파일 디렉터리에 사용
  config:
    import: optional:classpath:application-logging-defaults.yml
```

- `optional:` — classpath에 defaults가 없어도 기동 실패하지 않음 (의존성 누락 시 방어).
- 서비스 `src/main/resources/logback-spring.xml`은 **두지 않음** (classpath에 설정이 하나만 있어야 함).

---

## 3. 로그 패턴

콘솔·일반 파일·ERROR 파일 **동일 패턴** (`logback-spring.xml`의 `LOG_PATTERN`):

```
[${APP_NAME}] %-5level %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %logger{36} - %msg%n
```

### 필드 설명

| 요소 | 예시 | 의미 |
|------|------|------|
| `[${APP_NAME}]` | `[integrated-api]` | `spring.application.name` |
| `%-5level` | `INFO ` | 로그 레벨 (5자 왼쪽 정렬) |
| `%d{...}` | `2026-06-02 17:30:00.123` | 타임스탬프 (밀리초 포함) |
| `[%thread]` | `[http-nio-8081-exec-1]` | 스레드명 |
| `%logger{36}` | `c.b.i.c.service.CollectingJobService` | 로거명 (최대 36자) |
| `%msg` | 실제 메시지 | `log.info("...")` 본문 |
| `%n` | 줄바꿈 | |

### 출력 예

```text
[integrated-worker] INFO  2026-06-02 17:30:00.123 [scheduling-1] c.b.i.c.s.CollectingJobWorker - CollectingJob Worker call poll()
```

패턴 문자열 자체를 바꾸려면 **`logback-spring.xml` 수정 + `common-logging` 재빌드**가 필요합니다 (yml 키로 노출하지 않음).

---

## 4. Appender 동작 (출력 대상)

| Appender | 용도 | 활성 조건 |
|----------|------|-----------|
| `consoleAppender` | 표준 출력 | **항상** |
| `fileAppender` | 전 레벨 일반 로그 파일 | `logging.backend.file-appender-enabled=true` |
| `errorAppender` | **ERROR만** 별도 파일 | 파일 appender와 **항상 함께** on/off |

`file-appender-enabled=false` (기본·로컬) → **콘솔만**  
`file-appender-enabled=true` (`dev` / `prod` 프로파일) → 콘솔 + 일반 파일 + ERROR 파일

제어는 `application-logging-defaults.yml`의 두 번째 문서(프로파일 `dev,prod`)에서 `file-appender-enabled: true`로 켭니다.

---

## 5. 로그 파일 경로

### 디렉터리 구조

`logging.file.path` + `logging.file.name` (`spring.application.name`과 동일하게 맞춤):

```text
{logging.file.path}/{logging.file.name}/
  {logging.file.name}.log              # 일반 로그 (active)
  {logging.file.name}.2026-06-02.0.log.gz  # 롤링·압축 아카이브
  error/
    error.log                          # ERROR 전용 (active)
    error.2026-06-02.0.log.gz
```

### 기본값 (defaults yml)

| 항목 | 기본값 |
|------|--------|
| `logging.file.path` | `./logs` |
| `logging.file.name` | `${spring.application.name}` |

### 예: `integrated-api`, 프로파일 `prod`, cwd = 프로젝트 루트

```text
./logs/integrated-api/integrated-api.log
./logs/integrated-api/error/error.log
```

---

## 6. 롤링·보관·삭제 정책

`SizeAndTimeBasedRollingPolicy` 사용: **날짜 + 파일 크기** 기준 롤링, `.gz` 압축.

### 일반 로그 (`fileAppender`)

| yml 키 | 기본값 | 설명 |
|--------|--------|------|
| `logging.logback.rollingpolicy.max-file-size` | `100MB` | 단일 파일 최대 크기, 초과 시 인덱스(`%i`) 증가 |
| `logging.logback.rollingpolicy.max-history` | `90` | **보관 일수** (90일 지난 아카이브 삭제) |
| `logging.logback.rollingpolicy.total-size-cap` | `10GB` | 롤링 아카이브 **전체 용량 상한** |
| `logging.backend.rolling.file-name-pattern` | `.%d{yyyy-MM-dd}.%i.log.gz` | 롤링 파일명 **접미사** (앞에 `${LOG_FILE}` 자동 접합) |

실제 롤링 파일명 패턴:

```text
${LOG_DIR}/${LOG_FILE_NAME}${logging.backend.rolling.file-name-pattern}
# 예: ./logs/integrated-api/integrated-api.%d{yyyy-MM-dd}.%i.log.gz
```

`.gz` 포함 시 Logback이 **gzip 압축** 아카이브를 생성합니다.

### ERROR 전용 로그 (`errorAppender`)

| yml 키 | 기본값 | 설명 |
|--------|--------|------|
| `logging.backend.error.subdirectory` | `error` | 일반 로그 디렉터리 하위 폴더 |
| `logging.backend.error.base-name` | `error` | ERROR 파일 베이스명 → `error/error.log` |
| `logging.backend.error.total-size-cap` | `3GB` | ERROR 아카이브 전체 용량 상한 |
| (공유) `max-file-size`, `max-history` | 위와 동일 | 일반 로그와 **같은** 롤링 단위·보관 일수 |

ERROR 로그는 `LevelFilter`로 **ERROR만** 기록합니다. 동일 이벤트가 일반 파일에도 ERROR로 남을 수 있습니다 (의도된 중복).

---

## 7. 로그 레벨 기본값

`application-logging-defaults.yml`:

| yml 키 | 기본값 | 대상 |
|--------|--------|------|
| `logging.level.root` | `INFO` | 전체 root (`logback` root level에도 연결) |
| `logging.level.com.backend` | `INFO` | 프로젝트 코드 (`com.backend.*`) |
| `logging.level.org.springframework` | `WARN` | Spring 프레임워크 |
| `logging.level.org.hibernate.SQL` | `WARN` | SQL 로그 (기본 off에 가깝게) |

서비스·프로파일 yml에서 **더 구체적인 패키지**로 override 가능:

```yaml
logging:
  level:
    com.backend.integratedworker.collectingjob: DEBUG
    org.hibernate.SQL: DEBUG   # SQL 디버깅 시에만
```

---

## 8. 프로파일별 동작

`application-logging-defaults.yml`은 **multi-document YAML** (`---` 구분)입니다.

### 문서 1 — 모든 프로파일 공통

- 롤링·레벨·경로 기본값
- `logging.backend.file-appender-enabled: false`

### 문서 2 — `dev`, `prod` 활성 시

```yaml
spring:
  config:
    activate:
      on-profile: dev,prod
logging:
  backend:
    file-appender-enabled: true
```

| 실행 예 | 파일 로그 |
|---------|-----------|
| 프로파일 없음, `local` 등 | 없음 (콘솔만) |
| `--spring.profiles.active=dev` | 있음 |
| `--spring.profiles.active=prod` | 있음 |
| `staging`만 켠 경우 | **없음** (defaults에 없음 → 필요 시 yml에 프로파일 추가) |

---

## 9. 설정 변경 가이드

### A. 팀 전체 기본값 변경

`common-logging/src/main/resources/application-logging-defaults.yml` 수정 후 **재배포** (JAR에 defaults 포함).

### B. 서비스·환경만 변경

해당 서비스 `application-prod.yml` 등:

```yaml
logging:
  file:
    path: /var/log/backend
  logback:
    rollingpolicy:
      max-history: 30
      total-size-cap: 5GB
  backend:
    rolling:
      file-name-pattern: ".%d{yyyy-MM-dd}.%i.log.gz"
    error:
      total-size-cap: 1GB
```

### C. 배포 환경 변수 (JAR 동일, 재기동만)

Spring Boot relaxed binding 예:

```bash
SPRING_PROFILES_ACTIVE=prod
LOGGING_FILE_PATH=/var/log/backend
LOGGING_LOGBACK_ROLLINGPOLICY_MAX_HISTORY=30
LOGGING_BACKEND_FILE_APPENDER_ENABLED=true
LOGGING_LEVEL_COM_BACKEND=DEBUG
```

### D. xml 수정이 필요한 경우

다음은 **yml만으로 변경 불가** — `logback-spring.xml` + `common-logging` 재빌드:

- Appender 종류·구조 (예: JSON encoder, async)
- `LOG_PATTERN` 문자열
- `springProperty`에 연결되지 않은 새 옵션
- `file` / `error` 분리 규칙 자체 변경

새 yml 키를 쓰려면 xml에 `<springProperty source="..."/>` 추가가 필요합니다.

---

## 10. 로컬 실행 예시

```bash
# 콘솔만 (기본)
./gradlew :integrated-api:bootRun

# 파일 + ERROR 분리 + 롤링
./gradlew :integrated-api:bootRun --args='--spring.profiles.active=prod'

# 보관 7일만 테스트
./gradlew :integrated-worker:bootRun --args='--spring.profiles.active=prod --logging.logback.rollingpolicy.max-history=7'
```

---

## 11. xml ↔ yml 키 매핑 전체

| yml 속성 | logback 변수 / 용도 |
|----------|---------------------|
| `spring.application.name` | `APP_NAME`, 패턴 `[...]` |
| `logging.file.path` | `LOG_FILE_PATH` |
| `logging.file.name` | `LOG_FILE_NAME` |
| `logging.level.root` | root `level` |
| `logging.backend.file-appender-enabled` | `FILE_APPENDER_ENABLED` → file+error on/off |
| `logging.logback.rollingpolicy.max-file-size` | `ROLLING_MAX_FILE_SIZE` |
| `logging.logback.rollingpolicy.max-history` | `ROLLING_MAX_HISTORY` |
| `logging.logback.rollingpolicy.total-size-cap` | `ROLLING_TOTAL_SIZE_CAP` |
| `logging.backend.rolling.file-name-pattern` | `ROLLING_FILE_NAME_PATTERN` |
| `logging.backend.error.subdirectory` | `ERROR_SUBDIRECTORY` |
| `logging.backend.error.base-name` | `ERROR_BASE_NAME` |
| `logging.backend.error.total-size-cap` | `ERROR_ROLLING_TOTAL_SIZE_CAP` |

---

## 12. 기술 참고

### Janino `<if>`

`logging.backend.file-appender-enabled`에 따라 root appender 조합을 바꿉니다.  
`common-logging`에 `org.codehaus.janino:janino` 의존성이 있습니다.

### XML 주의

`logback-spring.xml` 속성값에 `&`를 쓸 때는 XML 이스케이프 (`&amp;`)가 필요합니다.  
프로파일 조건은 yml의 `file-appender-enabled`로 옮겨 두었습니다.

### GlobalExceptionHandler

`common-web`의 예외 처리 로그 정책(4xx `debug`, 미처리 `error`)은 이 문서와 **별개**이며, 동일 Logback 설정 위에서 동작합니다.

---

## 13. 체크리스트 (인프라 로깅 완료 여부)

- [x] `common-logging` 모듈, 4개 Boot 서비스 의존
- [x] 공통 `logback-spring.xml` (classpath 1개)
- [x] `application-logging-defaults.yml` import
- [x] 공통 로그 패턴
- [x] 롤링·보관·용량 yml 설정
- [x] ERROR 전용 파일 분리 (파일 로깅 시)
- [x] dev/prod 파일 로깅, 그 외 콘솔만
- [ ] 애플리케이션 코드 `@Slf4j` 메시지·레벨 가이드 (별도 작업)

---

## 14. 관련 파일 경로

| 경로 |
|------|
| `common-logging/build.gradle` |
| `common-logging/src/main/resources/logback-spring.xml` |
| `common-logging/src/main/resources/application-logging-defaults.yml` |
| `integrated-api/src/main/resources/application.yml` |
| `integrated-worker/src/main/resources/application.yml` |
| `interaction-service/src/main/resources/application.yml` |
| `user-service/src/main/resources/application.yml` |
