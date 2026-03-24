# backend

Gradle + Spring Boot 기반 멀티 모듈 백엔드 프로젝트입니다.  
기본 개발 환경은 Java 17, Spring Boot 3.5.x 기준입니다.

## 모듈 구조

현재 `settings.gradle` 기준 모듈은 아래와 같습니다.

- `common-db`: 공통 DB/JPA 엔티티 및 DB 관련 공용 모듈
- `user-service`: 사용자 도메인 서비스 (실행 모듈)
- `search-service`: 검색 도메인 서비스 (실행 모듈)
- `interaction-service`: 상호작용 도메인 서비스 (실행 모듈)
- `integrated-api`: 통합 API 서비스 (실행 모듈)
- `integrated-worker`: 통합 워커 서비스 (실행 모듈)

## 의존 관계

- `user-service` -> `common-db`
- `search-service` -> `common-db`
- `interaction-service` -> `common-db`
- `integrated-api` -> `common-db`
- `integrated-worker` -> `common-db`

`common-db`는 라이브러리 모듈(`jar`), 나머지는 실행 모듈(`bootJar`)로 동작합니다.

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
```

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

## 데이터베이스/Flyway 설정 참고

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
├─ common-db
├─ user-service
├─ search-service
├─ interaction-service
├─ integrated-api
├─ integrated-worker
└─ docker-backend
```
