# Integrated API 명세서

`integrated-api`는 수집 대상 기술 블로그 제공자(Provider) 등록, 크롤링할 소스(Source)의 세부 설정 관리, 수집 및 재색인 작업 수동 트리거 및 상태 모니터링을 담당하는 관리용 백엔드 API 서비스입니다.

## 공통 정보
- **기본 경로(Base Path)**: `/collect/v1` 또는 `/index/v1`
- **인증 방식**: HTTP 헤더에 JWT Bearer 토큰을 포함하여 전송합니다 (`Authorization: Bearer <token>`). 수집 작업 실행 및 재색인 작업 트리거 등의 행위는 관리자 권한 토큰이 필수적으로 요구됩니다.

---

## 1. 블로그 제공자 관리 API (`/collect/v1`)

### 1.1 제공자 등록 (Provider)
새로운 기술 블로그 제공자(예: Toss, Kakao)를 등록합니다.

- **엔드포인트**: `POST /collect/v1/providers`
- **인증 필요 여부**: 필요 없음 (관리자 기본)

#### 요청 본문 (Request Body)
```json
{
  "name": "Toss",
  "baseUrl": "https://toss.tech",
  "description": "토스 기술 블로그"
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "provider_id": "9b1deb4d-3b7d-4b83-9dd1-e00cf112aab2",
  "created_at": "2026-07-23T23:17:00+09:00"
}
```

---

### 1.2 제공자 목록 조회
등록된 모든 블로그 제공자 목록을 페이징 형태로 조회합니다.

- **엔드포인트**: `GET /collect/v1/providers`
- **인증 필요 여부**: 필요 없음

#### 쿼리 파라미터 (Query Parameters)
- `page` (선택, 기본값 `0`)
- `size` (선택, 기본값 `10`)

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 10,
  "items": [
    {
      "provider_id": "9b1deb4d-3b7d-4b83-9dd1-e00cf112aab2",
      "name": "Toss",
      "base_url": "https://toss.tech",
      "description": "토스 기술 블로그",
      "is_used": true, // 사용 여부
      "has_using_collect_source": true, // 이 제공자를 바라보는 수집 소스 존재 여부
      "using_collect_source_id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
      "created_at": "2026-07-23T23:17:00+09:00",
      "updated_at": "2026-07-23T23:17:00+09:00"
    }
  ],
  "has_next": false
}
```

---

### 1.3 제공자 단건 조회
특정 제공자의 세부 정보를 조회합니다.

- **엔드포인트**: `GET /collect/v1/providers/{id}`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
제공자 목록 조회 내부 `content` 배열의 단일 객체 형식과 동일합니다.

---

### 1.4 제공자 정보 수정
제공자의 베이스 URL, 상세 설명 및 활성화 여부를 변경합니다.

- **엔드포인트**: `PATCH /collect/v1/providers/{id}`
- **인증 필요 여부**: 필요 없음

#### 요청 본문 (Request Body)
```json
{
  "baseUrl": "https://toss.tech/blog",
  "description": "수정된 토스 기술 블로그 설명",
  "isUsed": true
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "provider_id": "9b1deb4d-3b7d-4b83-9dd1-e00cf112aab2",
  "updated_at": "2026-07-23T23:19:00+09:00"
}
```

---

### 1.5 제공자 삭제
등록된 제공자를 제거합니다.

- **엔드포인트**: `DELETE /collect/v1/providers/{id}`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

## 2. 수집 소스 관리 API (`/collect/v1`)

### 2.1 수집 소스 등록 (Collect Source)
특정 제공자 내에서 상세하게 크롤링할 대상 타겟 경로 및 스케줄 주기(MANUAL 혹은 CRON)를 정의합니다.

- **엔드포인트**: `POST /collect/v1/sources`
- **인증 필요 여부**: 필요 없음

#### 요청 본문 (Request Body)
```json
{
  "provider_id": "9b1deb4d-3b7d-4b83-9dd1-e00cf112aab2",
  "url": "https://toss.tech/blog",
  "schedule_type": "CRON", // 수집 스케줄 형태 (MANUAL, CRON)
  "cron_expression": "0 0 3 * * ?", // 매일 새벽 3시 실행 (CRON인 경우 필수)
  "cron_from_page": 1,
  "cron_to_page": 3
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "source_id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
  "created_at": "2026-07-23T23:20:00+09:00"
}
```

---

### 2.2 수집 소스 목록 조회
등록된 모든 수집 소스 목록을 페이징 조회합니다.

- **엔드포인트**: `GET /collect/v1/sources`
- **인증 필요 여부**: 필요 없음

#### 쿼리 파라미터 (Query Parameters)
- `page` (선택, 기본값 `0`), `size` (선택, 기본값 `10`)

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 10,
  "items": [
    {
      "source_id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
      "provider_id": "9b1deb4d-3b7d-4b83-9dd1-e00cf112aab2",
      "url": "https://toss.tech/blog",
      "schedule_type": "CRON",
      "cron_expression": "0 0 3 * * ?",
      "cron_from_page": 1,
      "cron_to_page": 3,
      "is_used": true,
      "created_at": "2026-07-23T23:20:00+09:00",
      "updated_at": "2026-07-23T23:20:00+09:00"
    }
  ],
  "has_next": false
}
```

---

### 2.3 수집 소스 단건 조회
특정 수집 소스의 설정을 상세 조회합니다.

- **엔드포인트**: `GET /collect/v1/sources/{id}`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
수집 소스 목록 조회 내부 `content` 배열의 단일 객체 형식과 동일합니다.

---

### 2.4 수집 소스 정보 수정
수집 소스의 URL, 스케줄 타입, 크론 주기 정보, 활성화 여부 등을 업데이트합니다.

- **엔드포인트**: `PATCH /collect/v1/sources/{id}`
- **인증 필요 여부**: 필요 없음

#### 요청 본문 (Request Body)
```json
{
  "url": "https://toss.tech/blog",
  "collect_schedule_type": "MANUAL",
  "cron_expression": null,
  "cron_from_page": null,
  "cron_to_page": null,
  "is_used": false
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "source_id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
  "updated_at": "2026-07-23T23:22:00+09:00"
}
```

---

### 2.5 수집 소스 삭제
등록된 수집 소스를 제거합니다.

- **엔드포인트**: `DELETE /collect/v1/sources/{id}`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

## 3. 수집 작업(Crawl Job) 실행 API (`/collect/v1`)

### 3.1 수집 작업 수동 실행 (Start)
특정 수집 소스에 대해 수집 작업(`CollectingJob`)을 수동으로 즉시 구동시킵니다. 만약 대상 수집 소스가 `CRON` 주기형일 경우, 이 요청을 보내면 백그라운드 자동 생성 스케줄러 라이프사이클(`isUsed=true`)이 활성화됨과 동시에 최초 1회 수집 작업이 시작됩니다.

- **엔드포인트**: `POST /collect/v1/sources/{source-id}/_start`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 경로 변수 (Path Variables)
| 파라미터명 | 타입 | 필수 여부 | 설명 |
|---|---|---|---|
| `source-id` | UUID | 필수 | 실행 타겟 수집 소스의 고유 ID |

#### 쿼리 파라미터 (Query Parameters)
| 파라미터명 | 타입 | 필수 여부 | 기본값 | 설명 |
|---|---|---|---|---|
| `from_page` | String | 선택 | *(지정된 서버 기본값)* | 수집을 개시할 페이지 번호 |
| `to_page` | String | 선택 | *(지정된 서버 기본값)* | 수집을 종료할 페이지 번호 |
| `force_recollect` | Boolean | 선택 | `false` | 이미 수집한 포스팅 글이라도 강제 재수집할지 여부 |

#### 응답 본문 (`200 OK`)
```json
{
  "job_id": "ff1b2011-89ab-4c12-9ab2-8d9e2b10ab12",
  "job_status": "PENDING" // 현재 수집 작업 상태 (PENDING, RUNNING, SUCCESS, FAIL)
}
```

---

### 3.2 수집 작업 자동 실행 중지 (Stop)
`CRON` 타입 수집 소스의 자동 스케줄러 반복 주기를 비활성화(`isUsed=false`) 시킵니다. `MANUAL` 타입 소스에 대해 이 요청을 보내면 로직 상 무시됩니다.

- **엔드포인트**: `POST /collect/v1/sources/{source-id}/_stop`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

### 3.3 수집 작업 목록 조회
백그라운드에서 실행되었거나 진행 중인 수집 작업 목록과 상태를 조회합니다.

- **엔드포인트**: `GET /collect/v1/jobs`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 10,
  "items": [
    {
      "job_id": "ff1b2011-89ab-4c12-9ab2-8d9e2b10ab12",
      "job_status": "SUCCESS",
      "from_page": 1,
      "to_page": 3,
      "collecting_status": "DONE", // 세부 작업 현황 (COLLECTING, PARSING, DONE)
      "triggered_by": "d290f1ee-6c54-4b01-90e6-d701748f0851", // 실행한 관리자 ID
      "total_count": 15,
      "collected_count": 15,
      "attempt_count": 1,
      "force_recollect": false,
      "error_message": null,
      "started_at": "2026-07-23T23:25:00+09:00",
      "ended_at": "2026-07-23T23:26:00+09:00"
    }
  ],
  "has_next": false
}
```

---

### 3.4 수집 작업 상세 조회
특정 수집 작업의 실시간 진행도 및 예외 메시지를 상세 조회합니다.

- **엔드포인트**: `GET /collect/v1/jobs/{id}`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
수집 작업 목록 조회 내부 `content` 배열의 단일 객체 형식과 동일합니다.

---

## 4. 재색인(Re-indexing) 실행 API (`/index/v1`)

### 4.1 수집 소스 전체 글 재색인
특정 수집 소스(`CollectSource`)를 통해 이미 긁어온 적이 있는 모든 포스팅 데이터에 대해 처음부터 인덱스 재빌드를 수행합니다.

- **엔드포인트**: `POST /index/v1/sources/{source-id}/_reindex`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
```json
{
  "job_id": "ee2b2011-92ab-4c12-9ab2-8d9e2b1090f2",
  "job_status": "PENDING"
}
```

---

### 4.2 특정 단일 포스팅 재색인
오류가 나거나 메타데이터가 어긋난 특정 포스팅 데이터 1건을 지정하여 검색 엔진에 재색인 처리를 트리거합니다.

- **엔드포인트**: `POST /index/v1/posts/{post-id}/_reindex`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
```json
{
  "job_id": "cc1b2011-89ab-4c12-9ab2-8d9e2b10ab88",
  "job_status": "PENDING"
}
```

---

### 4.3 색인 작업 목록 조회
백그라운드에서 돌고 있는 색인 작업(`IndexingJob`)들의 처리 현황 목록을 조회합니다.

- **엔드포인트**: `GET /index/v1/jobs`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 10,
  "items": [
    {
      "job_id": "ee2b2011-92ab-4c12-9ab2-8d9e2b1090f2",
      "job_status": "SUCCESS",
      "indexing_job_type": "ALL_SOURCE_POSTS", // 색인 유형 (ALL_SOURCE_POSTS, SINGLE_POST)
      "triggered_by": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "target_source_id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
      "target_post_id": null,
      "total_count": 150,
      "indexed_count": 150,
      "error_message": null,
      "started_at": "2026-07-23T23:30:00+09:00",
      "ended_at": "2026-07-23T23:31:30+09:00"
    }
  ],
  "has_next": false
}
```

---

### 4.4 색인 작업 상세 조회
특정 색인 작업에 대한 실시간 진행 상태와 상세 에러 메시지를 조회합니다.

- **엔드포인트**: `GET /index/v1/jobs/{id}`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
색인 작업 목록 조회 내부 `content` 배열의 단일 객체 형식과 동일합니다.

---

## 5. 수집 포스팅 및 검색 문서 상태 검사 API

### 5.1 수집 결과 포스팅 메타데이터 조회
크롤러를 통해 긁어온 원래 원본(HTML 파싱 및 메타 요약본) 데이터 1건의 상세 속성을 보여줍니다.

- **엔드포인트**: `GET /collect/v1/postings/{id}`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
```json
{
  "posting_id": "e00be928-87b4-4b07-9b2f-410a8d4b32b1",
  "collect_source_id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
  "title": "Spring Boot에서 Clean Architecture 적용하기",
  "url": "https://techblog.com/clean-architecture",
  "published_at": "2026-07-20",
  "thumbnail_url": "https://techblog.com/images/clean.png",
  "summary": "본 아티클에서는 스프링 부트 환경에서 클린 아키텍처를 구현하는 과정을...",
  "indexing_status": "INDEXED", // 색인 여부 (UNINDEXED, INDEXED, FAIL)
  "indexing_error_count": 0,
  "last_indexed_at": "2026-07-20T11:00:00+09:00",
  "last_collected_at": "2026-07-20T10:00:00+09:00",
  "last_collecting_job_id": "ff1b2011-89ab-4c12-9ab2-8d9e2b10ab12"
}
```

---

### 5.2 색인 문서 현황 조회 (실제 검색엔진에 적재된 상태)
실제 검색 엔진(Elasticsearch 및 관련 적재 스토리지)에 등록되어 검색 가능한 상태로 구성된 완성형 게시글 문서 1건을 조회합니다.

- **엔드포인트**: `GET /index/v1/postings/{posting-id}`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
```json
{
  "id": "e00be928-87b4-4b07-9b2f-410a8d4b32b1",
  "title": "Spring Boot에서 Clean Architecture 적용하기",
  "url": "https://techblog.com/clean-architecture",
  "thumbnail_url": "https://techblog.com/images/clean.png",
  "summary": "본 아티클에서는 스프링 부트 환경에서 클린 아키텍처를 구현하는 과정을...",
  "provider": "Toss",
  "status": "PUBLISHED",
  "published_at": "2026-07-20",
  "created_at": "2026-07-20T10:00:00+09:00",
  "updated_at": "2026-07-20T10:00:00+09:00"
}
```
