# User Service API 명세서

`user-service`는 회원 계정 관리, 인증(직접 로그인 및 Google OAuth 연동), 프로필 정보 변경 등을 담당하는 서비스입니다.

## 공통 정보
- **기본 경로(Base Path)**: `/user/v1` 또는 `/auth/v1` (리소스에 따라 다름)
- **인증 방식**: HTTP 헤더에 JWT Bearer 토큰을 포함하여 전송합니다 (`Authorization: Bearer <token>`).

---

## 1. 인증 관련 API (`/auth/v1`)

### 1.1 직접 로그인 (비밀번호 인증)
이미 회원가입된 계정의 ID와 비밀번호를 사용하여 인증을 수행하고 토큰을 발급받습니다.

- **엔드포인트**: `POST /auth/v1/auth/login`
- **인증 필요 여부**: 필요 없음

#### 요청 본문 (Request Body)
```json
{
  "login_id": "user123",
  "password": "bXlwYXNzd29yZDEyMw==" // Base64로 인코딩된 비밀번호
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.2 Google OAuth 콜백 (SNS 로그인)
구글 OAuth 인증 서버로부터 전달받은 Authorization Code를 식별용 ID 토큰으로 교환 및 검증하고, 회원 등록 혹은 로그인을 완료합니다.

- **엔드포인트**: `GET /auth/v1/oauth/google/callback`
- **인증 필요 여부**: 필요 없음

#### 쿼리 파라미터 (Query Parameters)
| 파라미터명 | 타입 | 필수 여부 | 설명 |
|---|---|---|---|
| `code` | String | 필수 | 구글 인증 서버가 반환한 Authorization Code |

#### 응답 본문 (`200 OK`)
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## 2. 회원 관리 API (`/user/v1`)

### 2.1 일반 회원가입
새로운 일반 사용자 계정을 생성합니다.

- **엔드포인트**: `POST /user/v1/users`
- **인증 필요 여부**: 필요 없음

#### 요청 본문 (Request Body)
```json
{
  "login_id": "user123",
  "password": "bXlwYXNzd29yZDEyMw==", // Base64로 인코딩된 비밀번호
  "password_confirm": "bXlwYXNzd29yZDEyMw==", // Base64로 인코딩된 비밀번호 확인
  "nickname": "홍길동"
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "user_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "created_at": "2026-07-23T23:15:00+09:00"
}
```

---

### 2.2 초기 운영자(ADMIN) 계정 부트스트랩
공개 HTTP API로 관리자를 만들지 않는다. `user-service` 기동 시 활성 ADMIN이 없으면 설정값으로 1회 생성한다.

| 환경변수 | 기본(local) | 기본(prod) | 설명 |
|---|---|---|---|
| `ADMIN_BOOTSTRAP_ENABLED` | `true` | `false` | 부트스트랩 활성 여부 |
| `ADMIN_LOGIN_ID` | `admin` | `admin` | 로그인 ID |
| `ADMIN_PASSWORD` | `admin` | (필수, 시크릿) | **평문** 비밀번호. DB에는 BCrypt 저장 |
| `ADMIN_NICKNAME` | `admin` | `admin` | 닉네임 |

- 활성 ADMIN이 이미 있으면 skip(idempotent).
- `enabled=true` 인데 `ADMIN_PASSWORD`가 비어 있으면 기동 실패.
- 상용 첫 배포: 시크릿에 `ADMIN_PASSWORD`를 넣고 `ADMIN_BOOTSTRAP_ENABLED=true`로 기동 → 생성 확인 후 `false`로 되돌리거나, 이후에도 enabled를 켜 둬도 재생성되지 않음.
- 로그인 API(`POST /auth/v1/auth/login`) 호출 시에는 동일 평문 비밀번호를 **Base64 인코딩**해 `password` 필드에 넣는다.

---

### 2.3 내 정보 조회 (프로필 조회)
현재 로그인된 사용자의 상세 프로필 정보를 조회합니다.

- **엔드포인트**: `GET /user/v1/users/me`
- **인증 필요 여부**: 필요함 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`200 OK`)
```json
{
  "user_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "user_type": "USER", // 사용자 유형 (USER, ADMIN)
  "nickname": "홍길동",
  "created_at": "2026-07-23T23:15:00+09:00",
  "updated_at": "2026-07-23T23:20:00+09:00",
  "last_login_at": "2026-07-23T23:30:00+09:00"
}
```

---

### 2.4 닉네임 수정
현재 로그인된 사용자의 닉네임을 변경합니다.

- **엔드포인트**: `PATCH /user/v1/users/me`
- **인증 필요 여부**: 필요함 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "nickname": "새로운닉네임"
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "user_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "updated_at": "2026-07-23T23:40:00+09:00"
}
```

---

### 2.5 비밀번호 변경
현재 로그인된 사용자의 비밀번호를 수정합니다.

- **엔드포인트**: `PATCH /user/v1/users/me/password`
- **인증 필요 여부**: 필요함 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "password": "bXlwYXNzd29yZDEyMw==", // Base64로 인코딩된 기존 비밀번호
  "new_password": "bmV3cGFzc3dvcmQxMjM=" // Base64로 인코딩된 새 비밀번호
}
```

#### 응답 본문 (`200 OK`)
```json
{
  "user_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "updated_at": "2026-07-23T23:45:00+09:00"
}
```

---

### 2.6 SNS 계정 통합 (OAuth 연동)
기존에 생성된 일반 비밀번호 계정에 구글 등의 외부 SNS 인증 정보(OAuth Access Token)를 통합하여 추가 로그인 수단을 연동합니다.

- **엔드포인트**: `POST /user/v1/users/me/merge-oauth`
- **인증 필요 여부**: 필요함 (`Authorization: Bearer <JWT>`)

#### 요청 본문 (Request Body)
```json
{
  "access_token": "google_oauth_access_token"
}
```

#### 응답 본문 (`202 Accepted`)
내용 없음.

---

### 2.7 회원 탈퇴
현재 로그인된 사용자의 계정을 영구적으로 삭제합니다.

- **엔드포인트**: `DELETE /user/v1/users/me`
- **인증 필요 여부**: 필요함 (`Authorization: Bearer <JWT>`)

#### 응답 본문 (`202 Accepted`)
내용 없음.
