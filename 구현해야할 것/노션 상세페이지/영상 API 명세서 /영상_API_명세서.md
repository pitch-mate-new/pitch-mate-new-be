# 영상 API 명세서

Base URL: `http://localhost:8080`

---

## 1. 영상 업로드

- **Method:** POST
- **URL:** `/api/videos`
- **인증 필요:** O
- **설명:** 영상 파일을 업로드하면 Session(회차)이 자동 생성됩니다.

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |
| Content-Type | String | O | multipart/form-data |

### Request Body (multipart)
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| file | File | O | 영상 파일 |
| title | String | X | 영상 제목 |
| description | String | X | 영상 설명 |

### Success Response (201)
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 영상 ID |
| userId | Long | 업로더 사용자 ID |
| uploaderNickname | String | 업로더 닉네임 |
| title | String | 영상 제목 |
| description | String | 영상 설명 |
| videoUrl | String | 영상 파일 URL |
| thumbnailUrl | String | 썸네일 URL |
| type | String | 영상 타입 (UPLOAD) |
| durationSeconds | Integer | 영상 길이 (초) |
| createdAt | DateTime | 업로드 일시 |

```json
{
  "code": 1000,
  "status": 201,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 1,
    "userId": 1,
    "uploaderNickname": "불꽃진호팬",
    "title": "발표 연습 1회",
    "description": "첫 번째 발표 연습",
    "videoUrl": "/videos/uuid_sample.mp4",
    "thumbnailUrl": null,
    "type": "UPLOAD",
    "durationSeconds": null,
    "createdAt": "2026-04-09T12:00:00"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 500 | 4016 | 파일 업로드에 실패했습니다 |

---

## 2. 녹화 영상 등록

- **Method:** POST
- **URL:** `/api/videos/record`
- **인증 필요:** O
- **설명:** 웹캠으로 녹화한 영상을 등록합니다. Session(회차) 자동 생성.

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |
| Content-Type | String | O | multipart/form-data |

### Request Body (multipart)
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| file | File | O | 녹화 영상 파일 |
| title | String | X | 영상 제목 |
| description | String | X | 영상 설명 |

### Success Response (201)
> 영상 업로드와 동일한 구조, type = "RECORD"

```json
{
  "code": 1000,
  "status": 201,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 2,
    "userId": 1,
    "uploaderNickname": "불꽃진호팬",
    "title": "면접 연습 녹화",
    "type": "RECORD",
    "createdAt": "2026-04-09T12:00:00"
  }
}
```

---

## 3. 내 영상 목록 조회

- **Method:** GET
- **URL:** `/api/videos/my`
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
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": [
    {
      "id": 1,
      "userId": 1,
      "uploaderNickname": "불꽃진호팬",
      "title": "발표 연습 1회",
      "type": "UPLOAD",
      "videoUrl": "/videos/uuid_sample.mp4",
      "createdAt": "2026-04-09T12:00:00"
    }
  ]
}
```

---

## 4. 영상 상세 조회

- **Method:** GET
- **URL:** `/api/videos/{videoId}`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| videoId | Long | O | 영상 ID |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 1,
    "userId": 1,
    "uploaderNickname": "불꽃진호팬",
    "title": "발표 연습 1회",
    "description": "첫 번째 발표 연습",
    "videoUrl": "/videos/uuid_sample.mp4",
    "thumbnailUrl": null,
    "type": "UPLOAD",
    "durationSeconds": null,
    "createdAt": "2026-04-09T12:00:00"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4008 | 영상을 찾을 수 없습니다 |

---

## 5. 영상 정보 수정

- **Method:** PUT
- **URL:** `/api/videos/{videoId}`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |
| Content-Type | String | O | application/json |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| videoId | Long | O | 영상 ID |

### Request Body
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | X | 변경할 제목 |
| description | String | X | 변경할 설명 |

```json
{
  "title": "수정된 제목",
  "description": "수정된 설명"
}
```

### Success Response (200)
> 수정된 VideoResponse 반환

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 403 | 4009 | 해당 영상에 접근 권한이 없습니다 |
| 404 | 4008 | 영상을 찾을 수 없습니다 |

---

## 6. 영상 삭제

- **Method:** DELETE
- **URL:** `/api/videos/{videoId}`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| videoId | Long | O | 영상 ID |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "영상이 삭제되었습니다.",
  "result": null
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 403 | 4009 | 해당 영상에 접근 권한이 없습니다 |
| 404 | 4008 | 영상을 찾을 수 없습니다 |
