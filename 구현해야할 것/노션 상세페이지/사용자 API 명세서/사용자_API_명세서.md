# 사용자 API 명세서

Base URL: `http://localhost:8080`

---

## 1. 내 정보 조회

- **Method:** GET
- **URL:** `/api/users/me`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| email | String | 이메일 |
| nickname | String | 닉네임 |
| role | String | 역할 (MENTOR / MENTEE) |
| profileImage | String | 프로필 이미지 URL |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "불꽃진호팬",
    "role": "MENTEE",
    "profileImage": "https://image-url.com"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4007 | 사용자를 찾을 수 없습니다 |

---

## 2. 사용자 프로필 조회

- **Method:** GET
- **URL:** `/api/users/{userId}`
- **인증 필요:** X

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Content-Type | String | O | application/json |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 조회 대상 사용자 ID |

### Request Example
```
GET /api/users/1
```

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "userId": 1,
    "nickname": "불꽃진호팬",
    "role": "MENTEE",
    "profileImage": "https://image-url.com"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 404 | 4007 | 사용자를 찾을 수 없습니다 |

---

## 3. 프로필 수정

- **Method:** PUT
- **URL:** `/api/users/me`
- **인증 필요:** O
- **설명:** 닉네임과 프로필 이미지 URL을 함께 수정합니다. (각각 선택)

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |
| Content-Type | String | O | application/json |

### Request Body
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| nickname | String | X | 변경할 닉네임 (2~30자) |
| profileImage | String | X | 프로필 이미지 URL |

```json
{
  "nickname": "새닉네임",
  "profileImage": "https://image-url.com"
}
```

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "프로필 수정 완료",
  "result": null
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 409 | 4002 | 이미 사용 중인 닉네임 |

---

## 4. 닉네임 변경 (Deprecated)

> PUT /api/users/me 사용 권장

- **Method:** PATCH
- **URL:** `/api/users/me/nickname`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |
| Content-Type | String | O | application/json |

### Request Body
```json
{
  "nickname": "변경닉네임"
}
```

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "닉네임 변경 완료",
  "result": null
}
```

---

## 5. 프로필 이미지 변경 (Deprecated)

> PUT /api/users/me 사용 권장

- **Method:** PATCH
- **URL:** `/api/users/me/profile-image`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |
| Content-Type | String | O | application/json |

### Request Body
```json
{
  "profileImage": "https://image-url.com"
}
```

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "프로필 이미지 변경 완료",
  "result": null
}
```

---

## 6. 회원 탈퇴

- **Method:** DELETE
- **URL:** `/api/users/me`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "회원 탈퇴가 완료되었습니다.",
  "result": null
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4007 | 사용자를 찾을 수 없습니다 |
