# Interaction Service API 명세서

`interaction-service`는 사용자가 기술 블로그 글을 검색하고 조회하며, 좋아요나 즐겨찾기를 등록하고 댓글/답글(대댓글)을 남기거나 부적절한 게시글/댓글을 신고하는 등 사용자와의 상호작용을 처리하는 서비스입니다.

## 공통 정보
- **기본 경로(Base Path)**: `/interaction/v1`
- **인증 방식**: HTTP 헤더에 JWT Bearer 토큰을 포함하여 전송합니다 (`Authorization: Bearer <token>`). 글 목록 검색이나 단건 조회는 인증 토큰 없이도 동작하지만, 좋아요, 즐겨찾기, 댓글 작성, 신고 등은 인증 토큰이 필수로 요구됩니다.

---

## 1. 포스팅 조회 및 검색 API

### 1.1 포스팅 목록 조회 (키워드 검색, 필터링 및 페이징)
수집 및 색인된 기술 블로그 게시글 목록을 페이지네이션 형태로 가져옵니다. 로그인 상태로 호출할 경우 각 게시글에 대해 현재 사용자가 좋아요 또는 즐겨찾기(북마크)를 등록했는지의 여부(`likes_of_me`, `bookmarks_of_me`)가 활성화됩니다.

- **엔드포인트**: `GET /interaction/v1/postings`
- **인증 필요 여부**: 선택 사항 (인증 토큰 누락 시 `likes_of_me`, `bookmarks_of_me`는 항상 `false`로 고정)

#### 쿼리 파라미터 (Query Parameters)
| 파라미터명 | 타입 | 필수 여부 | 기본값 | 설명 |
|---|---|---|---|---|
| `title` | String | 선택 | - | 게시글 제목 검색용 키워드 |
| `provider` | String | 선택 | - | 특정 블로그 제공자(예: Toss, Kakao) 필터링 |
| `page` | Integer | 선택 | `0` | 페이지 번호 (0부터 시작) |
| `size` | Integer | 선택 | `20` | 한 페이지당 가져올 게시글 수 |
| `sort` | String | 선택 | - | 정렬 기준 (예: `published_at,desc` - 발행일 최신순) |

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 95,
  "page": 0,
  "size": 20,
  "items": [
    {
      "id": "e00be928-87b4-4b07-9b2f-410a8d4b32b1",
      "title": "Spring Boot에서 Clean Architecture 적용하기",
      "url": "https://techblog.com/clean-architecture",
      "thumbnail_url": "https://techblog.com/images/clean.png",
      "summary": "본 아티클에서는 스프링 부트 환경에서 클린 아키텍처를 구현하는 과정을...",
      "provider": "Toss",
      "status": "PUBLISHED", // 게시 상태 (PUBLISHED, DELETED)
      "published_at": "2026-07-20",
      "created_at": "2026-07-20T10:00:00+09:00",
      "updated_at": "2026-07-20T10:00:00+09:00",
      "like_count": 15,
      "view_count": 120,
      "comment_count": 3,
      "total_report_count": 0,
      "likes_of_me": true,
      "bookmarks_of_me": false
    }
  ],
  "has_next": true
}
```

---

### 1.2 포스팅 단건 상세 조회
특정 게시글의 상세 정보를 가져옵니다. 조회에 성공할 때마다 조회수(view count)가 실시간으로 증가합니다.

- **엔드포인트**: `GET /interaction/v1/postings/{postId}`
- **인증 필요 여부**: 선택 사항

#### 경로 변수 (Path Variables)
| 파라미터명 | 타입 | 필수 여부 | 설명 |
|---|---|---|---|
| `postId` | UUID | 필수 | 게시글의 고유 ID |

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
  "updated_at": "2026-07-20T10:00:00+09:00",
  "like_count": 15,
  "view_count": 121, // 조회수가 1 증가함
  "comment_count": 3,
  "total_report_count": 0,
  "likes_of_me": true,
  "bookmarks_of_me": false
}
```

---

## 2. 포스팅 즐겨찾기(북마크) API

### 2.1 즐겨찾기 등록
게시글을 내 즐겨찾기에 등록합니다. (멱등성 보장: 동일한 요청을 여러 번 보내도 최종 상태는 동일함)

- **엔드포인트**: `POST /interaction/v1/postings/{postId}/bookmarks`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

### 2.2 즐겨찾기 해제(삭제)
게시글을 내 즐겨찾기에서 해제합니다. (멱등성 보장)

- **엔드포인트**: `DELETE /interaction/v1/postings/{postId}/bookmarks`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

### 2.3 내 즐겨찾기 목록 조회
현재 로그인한 사용자가 즐겨찾기한 게시글 목록을 즐겨찾기 등록일 내림차순(최신순)으로 조회합니다.

- **엔드포인트**: `GET /interaction/v1/me/bookmarks`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 쿼리 파라미터 (Query Parameters)
- `page` (선택, 기본값 `0`), `size` (선택, 기본값 `20`), `sort` (선택)

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 20,
  "items": [
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
      "updated_at": "2026-07-20T10:00:00+09:00",
      "like_count": 15,
      "view_count": 121,
      "comment_count": 3,
      "total_report_count": 0
    }
  ],
  "has_next": false
}
```

---

## 3. 포스팅 좋아요 API

### 3.1 좋아요 등록
게시글에 좋아요를 등록합니다. (멱등성 보장: 중복 호출해도 좋아요 수는 1번만 증가함)

- **엔드포인트**: `POST /interaction/v1/postings/{postId}/likes`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

### 3.2 좋아요 취소
게시글에 등록한 좋아요를 취소합니다. (멱등성 보장)

- **엔드포인트**: `DELETE /interaction/v1/postings/{postId}/likes`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

## 4. 댓글 및 대댓글 관리 API

### 4.1 댓글 작성 (1-Depth)
특정 게시글에 최상위 댓글을 작성합니다.

- **엔드포인트**: `POST /interaction/v1/postings/{postId}/comments`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "content": "좋은 글이네요! 유익하게 읽고 갑니다."
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
  "created_at": "2026-07-23T23:16:00+09:00"
}
```

---

### 4.2 댓글 목록 조회
특정 게시글에 작성된 최상위 댓글 목록을 등록 시간 오름차순(오래된 순)으로 조회합니다.

- **엔드포인트**: `GET /interaction/v1/postings/{postId}/comments`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 20,
  "items": [
    {
      "id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
      "user_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "has_child_comment": false, // 대댓글 존재 여부
      "content": "좋은 글이네요! 유익하게 읽고 갑니다.",
      "status": "ACTIVE", // 댓글 상태 (ACTIVE, DELETED)
      "created_at": "2026-07-23T23:16:00+09:00",
      "updated_at": "2026-07-23T23:16:00+09:00"
    }
  ],
  "has_next": false
}
```

---

### 4.3 댓글 수정
내가 작성한 댓글의 내용을 수정합니다.

- **엔드포인트**: `PATCH /interaction/v1/comments/{commentId}`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "content": "수정된 댓글 내용입니다."
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
  "updated_at": "2026-07-23T23:18:00+09:00"
}
```

---

### 4.4 댓글 삭제
내가 작성한 댓글을 삭제 처리(Soft Delete)합니다.

- **엔드포인트**: `DELETE /interaction/v1/comments/{commentId}`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

### 4.5 대댓글(답글) 작성 (2-Depth)
댓글 하위에 중첩되는 대댓글을 남깁니다. 부모 ID는 반드시 1-depth 댓글이어야 합니다.

- **엔드포인트**: `POST /interaction/v1/comments/{commentId}/replies`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "content": "의견 주신 내용에 동감합니다."
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "id": "ff1b2011-89ab-4c12-9ab2-8d9e2b10ab12",
  "parent_id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
  "created_at": "2026-07-23T23:20:00+09:00"
}
```

---

### 4.6 대댓글 목록 조회
특정 최상위 댓글 하위의 대댓글 목록을 등록된 순서(오래된 순)로 조회합니다.

- **엔드포인트**: `GET /interaction/v1/comments/{commentId}/replies`
- **인증 필요 여부**: 필요 없음

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 20,
  "items": [
    {
      "id": "ff1b2011-89ab-4c12-9ab2-8d9e2b10ab12",
      "user_id": "c88f1aa2-9bb4-411a-8bb4-098ad81190ab",
      "has_child_comment": false,
      "content": "의견 주신 내용에 동감합니다.",
      "status": "ACTIVE",
      "created_at": "2026-07-23T23:20:00+09:00",
      "updated_at": "2026-07-23T23:20:00+09:00"
    }
  ],
  "has_next": false
}
```

---

### 4.7 대댓글 수정
내가 작성한 대댓글을 수정합니다.

- **엔드포인트**: `PATCH /interaction/v1/replies/{replyId}`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "content": "수정된 대댓글 내용입니다."
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "id": "ff1b2011-89ab-4c12-9ab2-8d9e2b10ab12",
  "updated_at": "2026-07-23T23:22:00+09:00"
}
```

---

### 4.8 대댓글 삭제
내가 작성한 대댓글을 삭제 처리(Soft Delete)합니다.

- **엔드포인트**: `DELETE /interaction/v1/replies/{replyId}`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

### 4.9 내가 쓴 댓글/대댓글 전체 조회
현재 로그인한 사용자가 작성한 모든 댓글 및 대댓글을 최신순으로 조회합니다.

- **엔드포인트**: `GET /interaction/v1/me/comments`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 20,
  "items": [
    {
      "id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
      "user_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "has_child_comment": false,
      "content": "좋은 글이네요! 유익하게 읽고 갑니다.",
      "status": "ACTIVE",
      "created_at": "2026-07-23T23:16:00+09:00",
      "updated_at": "2026-07-23T23:16:00+09:00"
    }
  ],
  "has_next": false
}
```

---

## 5. 신고 및 어뷰징 제보 API

### 5.1 포스팅 신고
기술 블로그 글에 부적절한 어뷰징 항목(잘못된 정보, 만료된 링크 등)을 제보합니다.

- **엔드포인트**: `POST /interaction/v1/postings/{postId}/reports`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "report_type": "INVALID_CONTENT", // 신고 구분 (INVALID_CONTENT, BROKEN_LINK, OTHER)
  "content": "링크가 만료되어 접속할 수 없습니다."
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "id": "3bb8c982-fbb4-4011-8fe6-7bda8a11a2f1",
  "created_at": "2026-07-23T23:25:00+09:00"
}
```

---

### 5.2 포스팅 신고 목록 조회 (관리자용)
접수된 게시글 신고 건들을 필터링하여 조회합니다.

- **엔드포인트**: `GET /interaction/v1/admin/reports/postings`
- **인증 필요 여부**: 필수 (Admin 권한 토큰)

#### 쿼리 파라미터 (Query Parameters)
| 파라미터명 | 타입 | 필수 여부 | 설명 |
|---|---|---|---|
| `status` | String | 선택 | 처리 현황 (`PENDING`, `RESOLVED_DELETED`, `REJECTED_KEEP`) |
| `report_type` | String | 선택 | 어뷰징 유형 (`INVALID_CONTENT`, `BROKEN_LINK`, `OTHER`) |
| `start_date` | String | 선택 | 검색 대상 시작일 (포맷: `yyyy-MM-dd`) |
| `end_date` | String | 선택 | 검색 대상 종료일 (포맷: `yyyy-MM-dd`) |

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 20,
  "items": [
    {
      "id": "3bb8c982-fbb4-4011-8fe6-7bda8a11a2f1",
      "post_id": "e00be928-87b4-4b07-9b2f-410a8d4b32b1",
      "reporter_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "status": "PENDING",
      "report_type": "INVALID_CONTENT",
      "content": "링크가 만료되어 접속할 수 없습니다.",
      "created_at": "2026-07-23T23:25:00+09:00",
      "updated_at": "2026-07-23T23:25:00+09:00"
    }
  ],
  "has_next": false
}
```

---

### 5.3 포스팅 신고 상태 수정 (관리자용)
접수된 `PENDING` 상태의 신고 건에 대해 처리 결과(삭제 결정 `RESOLVED_DELETED` 또는 유지 `REJECTED_KEEP`)를 지정합니다.

- **엔드포인트**: `PATCH /interaction/v1/admin/reports/postings/{reportId}`
- **인증 필요 여부**: 필수 (Admin 권한 토큰)

#### 요청 본문 (Request Body)
```json
{
  "status": "RESOLVED_DELETED"
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "id": "3bb8c982-fbb4-4011-8fe6-7bda8a11a2f1",
  "status": "RESOLVED_DELETED",
  "updated_at": "2026-07-23T23:27:00+09:00"
}
```

---

### 5.4 댓글 신고
댓글이나 대댓글에 부적절한 요소를 신고합니다.

- **엔드포인트**: `POST /interaction/v1/comments/{commentId}/reports`
- **인증 필요 여부**: 필수 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "report_type": "OTHER", // 신고 유형 (POLITICAL, ADULT, OTHER)
  "content": "공격적이고 부적절한 표현이 담겨 있습니다."
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "id": "8bb9d102-ebb4-4c12-9ee6-8bda9a119abf",
  "created_at": "2026-07-23T23:30:00+09:00"
}
```

---

### 5.5 댓글 신고 목록 조회 (관리자용)
접수된 댓글 신고 건들을 조회합니다.

- **엔드포인트**: `GET /interaction/v1/admin/reports/comments`
- **인증 필요 여부**: 필수 (Admin 권한 토큰)

#### 쿼리 파라미터 (Query Parameters)
- `status` (선택: `PENDING`, `RESOLVED_DELETED`, `REJECTED_KEEP`)
- `report_type` (선택: `POLITICAL`, `ADULT`, `OTHER`)
- `start_date` (선택: `yyyy-MM-dd`)
- `end_date` (선택: `yyyy-MM-dd`)

#### 응답 본문 (`200 OK`)
```json
{
  "total_count": 1,
  "page": 0,
  "size": 20,
  "items": [
    {
      "id": "8bb9d102-ebb4-4c12-9ee6-8bda9a119abf",
      "comment_id": "a90b6338-7bb4-4903-8fe5-1a892b1122ab",
      "reporter_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "status": "PENDING",
      "report_type": "OTHER",
      "content": "공격적이고 부적절한 표현이 담겨 있습니다.",
      "created_at": "2026-07-23T23:30:00+09:00",
      "updated_at": "2026-07-23T23:30:00+09:00"
    }
  ],
  "has_next": false
}
```

---

### 5.6 댓글 신고 상태 수정 (관리자용)
접수된 댓글 신고의 상태를 수정합니다.

- **엔드포인트**: `PATCH /interaction/v1/admin/reports/comments/{reportId}`
- **인증 필요 여부**: 필수 (Admin 권한 토큰)

#### 요청 본문 (Request Body)
```json
{
  "status": "RESOLVED_DELETED"
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "id": "8bb9d102-ebb4-4c12-9ee6-8bda9a119abf",
  "status": "RESOLVED_DELETED",
  "updated_at": "2026-07-23T23:32:00+09:00"
}
```
