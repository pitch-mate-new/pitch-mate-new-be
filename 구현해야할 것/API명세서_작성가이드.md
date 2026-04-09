# 노션 API 명세서 작성 가이드

> 노션 표의 각 행 = API 하나
> 행 클릭하면 나오는 상세 페이지 = Request / Response 내용 작성

---

## 공통 응답 형식 (모든 API 상세 페이지 맨 위에 한 번 써두기)

### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

### 실패 응답
```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
}
```

### 인증이 필요한 API 공통 헤더
```
Authorization: Bearer {accessToken}
```

---

# 인증 API 명세서

## 노션 표에 입력할 내용 (행 단위)

| API 이름        | HTTP 메서드 | API Path                  | 상태 | 담당자 | 설명                       |
|---------------|-----------|---------------------------|------|------|--------------------------|
| 회원가입          | POST      | /api/auth/signup          | -    | -    | 이메일, 비밀번호, 닉네임으로 계정 생성   |
| 로그인           | POST      | /api/auth/login           | -    | -    | 로그인 후 액세스/리프레시 토큰 발급     |
| 로그아웃          | POST      | /api/auth/logout          | -    | -    | 리프레시 토큰 삭제               |
| 토큰 재발급        | POST      | /api/auth/reissue         | -    | -    | 액세스 토큰 만료 시 재발급          |
| 이메일 중복 확인     | GET       | /api/auth/check-email     | -    | -    | 회원가입 전 이메일 사용 가능 여부 확인   |
| 닉네임 중복 확인     | GET       | /api/auth/check-nickname  | -    | -    | 회원가입 전 닉네임 사용 가능 여부 확인   |

---

## 각 행 클릭 → 상세 페이지에 입력할 내용

---

### 1. 회원가입

```
POST /api/auth/signup
인증: 불필요
```

#### Request Body (application/json)
```json
{
  "email": "test@example.com",
  "password": "password123",
  "nickname": "홍길동"
}
```

| 필드       | 타입     | 필수 | 조건          |
|----------|--------|----|-------------|
| email    | String | ✅  | 이메일 형식      |
| password | String | ✅  | 8자 이상       |
| nickname | String | ✅  | 2자 이상 30자 이하 |

#### Response 201 Created
```json
{
  "success": true,
  "data": null,
  "message": "회원가입이 완료되었습니다."
}
```

#### 에러 응답
| 상황          | 상태코드 | message                    |
|-------------|------|----------------------------|
| 이메일 중복      | 409  | 이미 사용 중인 이메일입니다.           |
| 닉네임 중복      | 409  | 이미 사용 중인 닉네임입니다.           |
| 유효성 검사 실패   | 400  | 비밀번호는 8자 이상이어야 합니다. (등)     |

---

### 2. 로그인

```
POST /api/auth/login
인증: 불필요
```

#### Request Body (application/json)
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

| 필드       | 타입     | 필수 |
|----------|--------|----|
| email    | String | ✅  |
| password | String | ✅  |

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer"
  },
  "message": null
}
```

| 필드           | 타입     | 설명                       |
|--------------|--------|--------------------------|
| accessToken  | String | 액세스 토큰 (유효기간 30분)        |
| refreshToken | String | 리프레시 토큰 (유효기간 7일)        |
| tokenType    | String | 항상 "Bearer"              |

#### 에러 응답
| 상황              | 상태코드 | message                        |
|-----------------|------|--------------------------------|
| 이메일/비밀번호 불일치   | 401  | 이메일 또는 비밀번호가 올바르지 않습니다.       |

---

### 3. 로그아웃

```
POST /api/auth/logout
인증: 필요 (Authorization: Bearer {accessToken})
```

#### Request Body
없음

#### Response 200 OK
```json
{
  "success": true,
  "data": null,
  "message": "로그아웃되었습니다."
}
```

---

### 4. 토큰 재발급

```
POST /api/auth/reissue
인증: 불필요
```

#### Request Body (application/json)
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

| 필드           | 타입     | 필수 |
|--------------|--------|----|
| refreshToken | String | ✅  |

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer"
  },
  "message": null
}
```

#### 에러 응답
| 상황           | 상태코드 | message                   |
|--------------|------|---------------------------|
| 유효하지 않은 토큰  | 401  | 유효하지 않은 리프레시 토큰입니다.       |
| 만료된 토큰      | 401  | 만료된 리프레시 토큰입니다.           |

---

### 5. 이메일 중복 확인

```
GET /api/auth/check-email?email=test@example.com
인증: 불필요
```

#### Query Parameter
| 파라미터  | 타입     | 필수 | 설명         |
|-------|--------|----|------------|
| email | String | ✅  | 확인할 이메일 주소 |

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "available": true
  },
  "message": null
}
```

| 필드        | 타입      | 설명                         |
|-----------|---------|----------------------------|
| available | Boolean | true = 사용 가능 / false = 이미 사용 중 |

---

### 6. 닉네임 중복 확인

```
GET /api/auth/check-nickname?nickname=홍길동
인증: 불필요
```

#### Query Parameter
| 파라미터     | 타입     | 필수 | 설명        |
|----------|--------|----|-----------|
| nickname | String | ✅  | 확인할 닉네임   |

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "available": true
  },
  "message": null
}
```

| 필드        | 타입      | 설명                         |
|-----------|---------|----------------------------|
| available | Boolean | true = 사용 가능 / false = 이미 사용 중 |

---

## 참고: 노션 기존 표 수정 사항

기존 노션 표에서 아래 항목 수정 필요:
- ❌ 닉네임 중복 확인 → Path가 `/api/auth/check-email` 로 잘못 되어있음
- ✅ 올바른 Path: `/api/auth/check-nickname`
- 상태 "Deprecated" → 구현 완료된 것들은 변경 필요
