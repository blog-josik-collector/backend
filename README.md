# backend

Gradle 기반 Spring Boot 멀티 모듈 프로젝트입니다.  
JDK 17 기준으로 구성되어 있습니다.

## Modules

- `user-service`
- `search-service`
- `interaction-service`
- `integrated-common`
- `integrated-worker` (실행 모듈)
- `integrated-api` (실행 모듈)

## 의존 관계

- `integrated-worker` -> `integrated-common`
- `integrated-api` -> `integrated-common`

## Gradle 설정 상세 설명

이 프로젝트는 "공통 설정은 루트에서, 모듈별 특화 설정은 각 서브 모듈에서"라는 원칙으로 구성되어 있습니다.

### 1) 루트 `build.gradle`

루트 `build.gradle`은 전체 모듈에 공통으로 적용되는 기준 설정을 담당합니다.

#### plugins

- `id "java"`
  - 루트 프로젝트 자체를 Java 프로젝트로 다루기 위한 기본 플러그인입니다.
- `id "org.springframework.boot" version "3.3.5" apply false`
  - Spring Boot 플러그인의 버전만 루트에서 통합 관리합니다.
  - `apply false`라서 루트에는 직접 적용되지 않고, 각 서브 모듈에서 `id "org.springframework.boot"`를 선언할 때 동일 버전이 사용됩니다.
- `id "io.spring.dependency-management" version "1.1.6" apply false`
  - Spring Boot BOM 기반 버전 관리를 위한 플러그인입니다.
  - 마찬가지로 버전만 루트에서 통일하고, 실제 적용은 서브 모듈에서 수행됩니다.

#### allprojects

- `group = "com.backend"`
  - 전체 모듈의 기본 그룹 ID를 통일합니다.
- `version = "0.0.1-SNAPSHOT"`
  - 전체 모듈의 기본 버전을 통일합니다.
- `repositories { mavenCentral() }`
  - 모든 모듈이 Maven Central에서 라이브러리를 내려받도록 공통 저장소를 지정합니다.

#### subprojects

`subprojects` 블록은 루트를 제외한 모든 서브 모듈에 일괄 적용됩니다.

- `apply plugin: "java"`
  - 모든 모듈을 Java 컴파일 대상으로 맞춥니다.
- `apply plugin: "io.spring.dependency-management"`
  - 모든 모듈이 Spring 의존성 버전 정렬(BOM)을 사용하도록 통일합니다.

- `java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }`
  - 모든 서브 모듈이 JDK 17 기준으로 컴파일/빌드되도록 강제합니다.
  - 팀/CI 환경에서 로컬 JDK 차이로 인한 빌드 불일치를 줄여줍니다.

- `configurations { compileOnly { extendsFrom annotationProcessor } }`
  - `annotationProcessor` 의존성을 `compileOnly`에도 연결합니다.
  - Lombok 같은 어노테이션 처리 라이브러리 사용 시 편의성을 높이는 일반적인 패턴입니다.

- `dependencies`
  - `implementation "org.springframework.boot:spring-boot-starter"`
    - 전 모듈 공통 런타임/기본 Spring 의존성을 루트에서 한 번만 선언해 중복을 제거합니다.
  - `testImplementation "org.springframework.boot:spring-boot-starter-test"`
    - 테스트 공통 의존성(JUnit, AssertJ, Mock 등)을 모든 모듈에 제공합니다.
  - `testRuntimeOnly "org.junit.platform:junit-platform-launcher"`
    - JUnit Platform 런처를 런타임에만 사용하도록 설정합니다.

### 2) 서브 모듈 `build.gradle`

각 모듈 `build.gradle`은 "모듈별로 달라지는 부분"만 정의합니다.

#### `user-service`, `search-service`, `interaction-service`, `integrated-common`

공통 형태:

- `plugins { id "org.springframework.boot" }`
  - Spring Boot 관련 태스크/설정을 사용할 수 있게 합니다.
- `bootJar { enabled = false }`
  - 실행 가능한 fat jar 생성 비활성화
- `jar { enabled = true }`
  - 일반 라이브러리 jar 생성 활성화

즉, 위 4개 모듈은 애플리케이션 실행 모듈이 아니라, 다른 모듈에서 참조하는 라이브러리 모듈로 동작합니다.

#### `integrated-worker`

- `plugins { id "org.springframework.boot" }`
- `dependencies { implementation project(":integrated-common") }`

설명:

- `integrated-worker`는 실행 모듈이므로 `bootJar`를 끄지 않습니다(기본값으로 실행 jar 생성).
- `integrated-common` 코드를 재사용하기 위해 프로젝트 의존성을 선언합니다.

#### `integrated-api`

- `plugins { id "org.springframework.boot" }`
- `dependencies`
  - `implementation project(":integrated-common")`
  - `implementation "org.springframework.boot:spring-boot-starter-web"`

설명:

- `integrated-api`도 실행 모듈이며, 웹 API 제공을 위해 `spring-boot-starter-web`를 모듈 특화 의존성으로 추가합니다.
- 공통 로직은 `integrated-common`에서 가져옵니다.

### 3) 왜 이런 구조가 좋은가?

- 공통 의존성/버전/JDK를 루트에서 통합 관리하므로 유지보수가 쉬워집니다.
- 모듈별 `build.gradle`은 짧고 명확해져서 역할이 분리됩니다.
- 라이브러리 모듈과 실행 모듈이 명확하게 구분되어 빌드 산출물(`jar`, `bootJar`)이 의도대로 생성됩니다.

## Run

`integrated-worker` 또는 `integrated-api` 모듈을 실행하면 됩니다.

```bash
./gradlew :integrated-worker:bootRun
./gradlew :integrated-api:bootRun
```
