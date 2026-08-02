# Integrated Worker 구조 및 스케줄링 가이드

`integrated-worker`는 Spring Framework의 `@Scheduled` 어노테이션을 사용하여 백그라운드 작업을 주기적으로 처리하는 서비스입니다. 해당 서비스는 외부에 HTTP API를 직접 노출하지 않고, 다음과 같은 백그라운드 업무를 전담합니다.
1. 활성화된 스케줄 주기(`CRON`) 수집 소스로부터 주기적으로 크롤링 작업(`CollectingJob`) 자동 스케줄링 생성.
2. Selenium Headless Chrome 브라우저를 구동하여 블로그 게시글 크롤링 실행.
3. 접수된 수동/자동 재색인 작업을 대기열에서 픽업하여 검색 엔진 데이터 동기화 및 정합성 조율 수행.

---

## 1. 백그라운드 워커 엔진 & 트리거 종류

서비스 내부에는 다음과 같이 병렬로 폴링하며 동작하는 네 가지 핵심 워커가 존재합니다.

### 1.1 크론 수집 작업 생성기 (`CollectingJobCronCreationWorker`)
- **트리거 방식**: 백그라운드 주기적 폴링
- **주기**: `${collecting-job-cron-generator.schedule-delay}` 설정 주기
- **주요 기능**:
  - 데이터베이스의 활성화된 `CRON` 형식의 수집 소스(`CollectSource`) 목록을 가져옵니다.
  - Spring의 `CronExpression.parse()`를 이용하여 현재 시간이 해당 소스의 수집 실행 주기에 해당하는지 검사합니다.
  - 실행 주기 조건에 부합하고, 해당 소스로 현재 진행 중인 액티브 수집 작업이 없을 경우, `PENDING` 상태의 새로운 `CollectingJob` 데이터를 생성하여 스케줄링 대기열에 등록합니다.

### 1.2 수집 작업 실행기 (`CollectingJobWorker`)
- **트리거 방식**: 백그라운드 주기적 폴링
- **주기**: `${collecting-job-worker.schedule-delay}` 설정 주기
- **주요 기능**:
  - `${collecting-job-worker.job-batch-size}`로 지정된 개수만큼 `PENDING` 상태의 수집 작업을 한 번에 픽업합니다.
  - 픽업한 작업들을 `RUNNING` 상태로 변경하고, `CollectingJobExecutor`를 통해 비동기 스레드 풀(`@Async("collectingExecutor")`)에서 실제 크롤링 로직을 수행합니다.

### 1.3 색인 작업 실행기 (`IndexingJobWorker`)
- **트리거 방식**: 백그라운드 주기적 폴링
- **주기**: `${indexing-job-worker.schedule-delay}` 설정 주기
- **주요 기능**:
  - **수동(MANUAL) 색인**: 운영자가 관리 콘솔(API)을 통해 직접 트리거한 PENDING 상태의 색인 작업을 가져와 `IndexingJobExecutor`를 통해 비동기로 실행합니다.
  - **자동(CRON) 색인**: 크롤링 결과 새로 유입된 글 중 색인 상태가 대기 중인 글(`indexingStatus == PENDING`)이 있는지 검사합니다. 만약 새 글이 존재하면 이들을 그룹화하여 `CRON` 형식의 색인 작업 데이터를 동적으로 만든 뒤 즉시 실시간 색인을 비동기 구동합니다.

### 1.4 색인 정합성 조율기 (`IndexingReconciliationWorker`)
- **트리거 방식**: 백그라운드 주기적 폴링
- **주기**: `${indexing-reconciliation.schedule-delay}` 설정 주기
- **주요 기능**: 크롤링 데이터 원본과 실제 검색엔진에 인덱싱 적재된 데이터 간에 차이가 발생하지 않도록 주기적인 데이터 비교 및 보정(Reconciliation) 정합성 싱크 작업을 처리합니다.

---

## 2. 크롤링 엔진 설계 및 웹 파싱 로직

수집 작업이 실제로 실행되면, `BlogCrawlerService`는 동적인 JavaScript 렌더링이 이루어지는 블로그 페이지를 긁어오기 위해 **Selenium WebDriver 기반 Headless Chrome 브라우저**를 띄웁니다.

### 2.1 공통 크롤링 실행 흐름
1. **드라이버 준비**: `WebDriverManager`를 구동하여 실행 환경에 맞는 최신 Chrome WebDriver를 실시간 다운로드 및 설정합니다.
2. **브라우저 실행**: UI가 노출되지 않는 헤드리스 모드(`--headless=new`) 옵션으로 크롬 브라우저 프로세스를 기동합니다.
3. **페이지 접속 및 렌더링 대기**: 타겟 블로그 주소의 페이지별 URL로 이동한 뒤, 동적 JS 리소스들이 완전히 로딩되도록 **5초간 대기(Sleep)**합니다.
4. **선택자 파싱**: 각 블로그 엔진에 할당된 고유 CSS Selector를 적용하여 게시글 래퍼 요소 리스트를 수집합니다.
5. **메타 추출**: 파싱한 HTML 엘리먼트로부터 게시글 `제목`, `본문 요약`, `썸네일 이미지 주소`, `원문 링크`, `발행 일자` 등을 읽어옵니다.
6. **데이터 저장**: 중복 적재 확인을 거친 후 DB에 `CollectSourcePost` 객체로 최종 삽입 처리합니다.

### 2.2 제공자별 크롤링 전략 세부 구성

제공자 이름(`PostProvider.name`) 값에 따라 서로 다른 크롤러 전략이 주입되어 처리됩니다.

| 블로그 제공자명 | 처리 전략 클래스명 | 크롤링 대상 사이트 | 동작 모델 및 기술 |
|---|---|---|---|
| `kakao` | `KakaoBlogCrawler` | 카카오 기술 블로그 | Headless Selenium (DOM 파싱 및 JS 대기 렌더링) |
| `line` | `LineBlogCrawler` | 라인 엔지니어링 블로그 | Headless Selenium (DOM 파싱 및 JS 대기 렌더링) |
| `toss` | `TossBlogCrawler` | 토스 기술 블로그 | Headless Selenium (DOM 파싱 및 JS 대기 렌더링) |
