# 인증 API 명세서

Base URL: `http://localhost:8080`

---

## 1. 회원가입

- **Method:** POST
- **URL:** `/api/auth/signup`
- **인증 필요:** X

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Content-Type | String | O | application/json |

### Request Body
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | O | 사용자 이메일 |
| password | String | O | 비밀번호 (8자 이상) |
| nickname | String | O | 사용자 닉네임 (2~30자) |
| role | String | X | 역할 (MENTOR / MENTEE), 기본값: MENTEE |

### Request Example
```json
{
  "email": "user@example.com",
  "password": "Abcd1234!",
  "nickname": "불꽃진호팬",
  "role": "MENTEE"
}
```

### Success Response (201)
| 이름 | 타입 | 설명 |
|------|------|------|
| userId | Long | 생성된 사용자 ID |
| email | String | 가입 이메일 |
| nickname | String | 사용자 닉네임 |
| role | String | 사용자 역할 |

```json
{
  "code": 1000,
  "status": 201,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "불꽃진호팬",
    "role": "MENTEE"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 400 | 4000 | 유효성 검사 실패 |
| 409 | 4001 | 이미 사용 중인 이메일 |
| 409 | 4002 | 이미 사용 중인 닉네임 |
| 500 | 5000 | 서버 내부 오류 |

---

## 2. 로그인

- **Method:** POST
- **URL:** `/api/auth/login`
- **인증 필요:** X

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Content-Type | String | O | application/json |

### Request Body
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | O | 사용자 이메일 |
| password | String | O | 사용자 비밀번호 |

### Request Example
```json
{
  "email": "user@example.com",
  "password": "Abcd1234!"
}
```

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| accessToken | String | 액세스 토큰 (30분) |
| refreshToken | String | 리프레시 토큰 (7일) |
| userId | Long | 사용자 ID |
| nickname | String | 사용자 닉네임 |
| role | String | 사용자 역할 |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "userId": 1,
    "nickname": "불꽃진호팬",
    "role": "MENTEE"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 400 | 4000 | 유효성 검사 실패 |
| 401 | 4003 | 이메일 또는 비밀번호가 올바르지 않습니다 |
| 500 | 5000 | 서버 내부 오류 |

---

## 3. 로그아웃

- **Method:** POST
- **URL:** `/api/auth/logout`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |
| Content-Type | String | O | application/json |

### Request Body
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | String | O | 무효화할 리프레시 토큰 |

```json
{
  "refreshToken": "eyJhbGci..."
}
```

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "로그아웃되었습니다.",
  "result": null
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 500 | 5000 | 서버 내부 오류 |

---

## 4. 토큰 재발급

- **Method:** POST
- **URL:** `/api/auth/reissue`
- **인증 필요:** X

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Content-Type | String | O | application/json |

### Request Body
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | String | O | 재발급에 사용할 현재 리프레시 토큰 |

```json
{
  "refreshToken": "eyJhbGci..."
}
```

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| accessToken | String | 새 액세스 토큰 |
| refreshToken | String | 새 리프레시 토큰 |
| userId | Long | 사용자 ID |
| nickname | String | 사용자 닉네임 |
| role | String | 사용자 역할 |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "userId": 1,
    "nickname": "불꽃진호팬",
    "role": "MENTEE"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4004 | 유효하지 않은 리프레시 토큰 |
| 401 | 4005 | 만료된 리프레시 토큰 |
| 500 | 5000 | 서버 내부 오류 |

---

## 5. 이메일 중복 확인

- **Method:** GET
- **URL:** `/api/auth/check-email`
- **인증 필요:** X

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Content-Type | String | O | application/json |

### Query Parameter
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | O | 중복 확인할 이메일 |

### Request Example
```
GET /api/auth/check-email?email=user@example.com
```

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| isDuplicated | Boolean | 이메일 중복 여부 (true: 중복, false: 사용 가능) |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "isDuplicated": true
  }
}
```

---

## 6. 닉네임 중복 확인

- **Method:** GET
- **URL:** `/api/auth/check-nickname`
- **인증 필요:** X

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Content-Type | String | O | application/json |

### Query Parameter
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| nickname | String | O | 중복 확인할 닉네임 |

### Request Example
```
GET /api/auth/check-nickname?nickname=불꽃진호팬
```

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| isDuplicated | Boolean | 닉네임 중복 여부 (true: 중복, false: 사용 가능) |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "isDuplicated": false
  }
}
```
