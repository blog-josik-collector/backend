# backend

Gradle + Spring Boot 기반 멀티 모듈 백엔드 프로젝트입니다.  
기본 개발 환경은 Java 17, Spring Boot 3.5.x 기준입니다.

## 모듈 구조

현재 `settings.gradle` 기준 모듈은 아래와 같습니다.

- `common-data-access`: 공통 데이터 접근(JPA 엔티티, persistence/security/exception 패키지)
- `user-service`: 사용자 도메인 서비스 (실행 모듈)
- `search-service`: 검색 도메인 서비스 (실행 모듈)
- `interaction-service`: 상호작용 도메인 서비스 (실행 모듈)
- `integrated-api`: 통합 API 서비스 (실행 모듈)
- `integrated-worker`: 통합 워커 서비스 (실행 모듈)
- `api-gateway`: 프론트 진입점 (Spring Cloud Gateway, `localhost:8000`)

## 의존 관계

- `user-service` -> `common-data-access`
- `search-service` -> `common-data-access`
- `interaction-service` -> `common-data-access`
- `integrated-api` -> `common-data-access`
- `integrated-worker` -> `common-data-access`
- `api-gateway`: 다른 모듈에 의존하지 않음. `/auth/v1`, `/user/v1` → user-service, `/interaction/v1` → interaction-service, `/collect/v1`·`/index/v1` → integrated-api

`common-data-access`는 라이브러리 모듈(`jar`), 나머지는 실행 모듈(`bootJar`)로 동작합니다.

## 기술 스택

- Java 17 (Gradle Toolchain)
- Spring Boot 3.5.12
- Spring Data JPA
- Flyway
- PostgreSQL
- Elasticsearch 8.18
- Redis

## 로컬 인프라 실행

`docker-backend/docker-compose.yml`에 테스트용 인프라가 정의되어 있습니다.

- PostgreSQL `15.7` (`localhost:5432`)
- Elasticsearch `8.18.0` (`localhost:9200`)
- Redis `7-alpine` (`localhost:6379`)

실행:

```bash
docker compose -f docker-backend/docker-compose.yml up -d
```

중지:

```bash
docker compose -f docker-backend/docker-compose.yml down
```

## 애플리케이션 실행

루트에서 원하는 모듈을 지정해 실행합니다.

```bash
./gradlew :user-service:bootRun
./gradlew :search-service:bootRun
./gradlew :interaction-service:bootRun
./gradlew :integrated-api:bootRun
./gradlew :integrated-worker:bootRun
./gradlew :api-gateway:bootRun
```

프론트는 `http://localhost:8000` 만 호출한다. 게이트웨이가 경로 그대로 각 서비스로 넘긴다 (`StripPrefix` 없음).

| 경로 | 대상 |
|------|------|
| `/auth/v1/**`, `/user/v1/**` | user-service `:8080` |
| `/interaction/v1/**` | interaction-service `:8083` |
| `/collect/v1/**`, `/index/v1/**` | integrated-api `:8081` |

Google OAuth 콜백도 게이트웨이 기준이다 (`http://localhost:8000/auth/v1/oauth/google/callback`). Swagger는 기존처럼 각 서비스 포트로 연다.

## 빌드/테스트

전체 빌드:

```bash
./gradlew clean build
```

전체 테스트:

```bash
./gradlew test
```

특정 모듈 테스트:

```bash
./gradlew :user-service:test
```

Jacoco LINE 커버리지 게이트 (Service 계층~, Repository 제외, 기준 ≥ 80%):

```bash
./scripts/check-coverage.sh                 # user / integrated-api / worker / interaction
./scripts/check-coverage.sh user-service    # 특정 모듈만
```

커버리지 현황: [docs/test-coverage.md](./docs/test-coverage.md)

## CI

GitHub Actions (`.github/workflows/ci.yml`): PR/`main`에서 `./gradlew test`만 실행한다. 커버리지는 로컬에서 `./scripts/check-coverage.sh`로 확인한다.

## Docker Compose (전체 스택)

| 파일 | 용도 |
|------|------|
| `docker-backend/docker-compose.yml` | 로컬 개발용 인프라만 (Postgres / Redis / ES) |
| `docker-compose.dev.yml` | **로컬과 같은 기본 프로필**로 앱+인프라 전부 (VM 검증용) |
| `docker-compose.prod.yml` | 서버 배포용 (`.env` 시크릿 필수) |

### VM에서 로컬과 동일 설정으로 검증

`prod` 프로필을 쓰지 않는다. DB 계정 `postgres/postgres`, Flyway·admin bootstrap·ES provisioning은 `application.yml` 기본값 그대로.

```bash
docker compose -f docker-compose.dev.yml up -d --build
# API: http://<VM-IP>:8000
# (선택) 인프라 포트도 노출: 5432, 6379, 9200
```

`.env` 없이도 기동된다. OAuth redirect 등만 바꿀 때는 `.env.dev.example` 참고.

### 서버 배포 (prod)

공개 포트는 게이트웨이 `8000`뿐이다.

```bash
cp .env.example .env   # 시크릿 채우기
docker compose -f docker-compose.prod.yml up -d --build
```

- DB를 사용하는 서비스(`user-service`, `search-service`, `interaction-service`)는 `application.yml`의 `spring.datasource`를 기준으로 PostgreSQL에 연결됩니다.
- Flyway 사용 시 PostgreSQL 환경에서는 `flyway-core`와 함께 `flyway-database-postgresql` 의존성이 필요합니다.
- 로컬 Docker 기본값을 사용할 경우 일반적으로 아래 값으로 맞춰 사용합니다.
  - URL: `jdbc:postgresql://localhost:5432/backend_db`
  - USERNAME: `postgres`
  - PASSWORD: `postgres`

## 디렉토리 예시

```text
backend
├─ build.gradle
├─ settings.gradle
├─ common-data-access
├─ user-service
├─ api-gateway
├─ search-service
├─ interaction-service
├─ integrated-api
├─ integrated-worker
├─ docker
├─ docker-compose.dev.yml
├─ docker-compose.prod.yml
└─ docker-backend
```
