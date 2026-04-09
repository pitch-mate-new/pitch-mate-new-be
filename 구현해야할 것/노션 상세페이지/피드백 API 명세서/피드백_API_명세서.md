# 피드백 API 명세서

Base URL: `http://localhost:8080`

---

## 1. 멘토 직접 피드백 작성

- **Method:** POST
- **URL:** `/api/videos/{videoId}/feedbacks`
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
| startTimeSeconds | Double | X | 구간 시작 시간 (초) |
| endTimeSeconds | Double | X | 구간 종료 시간 (초) |
| content | String | O | 피드백 내용 |

```json
{
  "startTimeSeconds": 10.5,
  "endTimeSeconds": 25.0,
  "content": "이 구간에서 말하는 속도가 너무 빠릅니다."
}
```

### Success Response (201)
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 피드백 ID |
| videoId | Long | 영상 ID |
| authorId | Long | 작성자 ID |
| authorNickname | String | 작성자 닉네임 |
| startTimeSeconds | Double | 구간 시작 시간 |
| endTimeSeconds | Double | 구간 종료 시간 |
| content | String | 피드백 내용 |
| type | String | 피드백 타입 (MANUAL) |
| createdAt | DateTime | 작성 일시 |

```json
{
  "code": 1000,
  "status": 201,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 1,
    "videoId": 1,
    "authorId": 2,
    "authorNickname": "멘토닉네임",
    "startTimeSeconds": 10.5,
    "endTimeSeconds": 25.0,
    "content": "이 구간에서 말하는 속도가 너무 빠릅니다.",
    "type": "MANUAL",
    "createdAt": "2026-04-09T12:00:00"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 400 | 4000 | 유효성 검사 실패 |
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4008 | 영상을 찾을 수 없습니다 |

---

## 2. AI 구간 피드백 생성

- **Method:** POST
- **URL:** `/api/videos/{videoId}/feedbacks/ai`
- **인증 필요:** O
- **설명:** Gemini AI가 영상을 분석하여 타임스탬프별 피드백을 3~5개 자동 생성합니다.

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| videoId | Long | O | 영상 ID |

### Success Response (201)
> FeedbackResponse 배열 (type = "AI")

```json
{
  "code": 1000,
  "status": 201,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": [
    {
      "id": 2,
      "videoId": 1,
      "authorId": null,
      "authorNickname": "AI",
      "startTimeSeconds": 5.0,
      "endTimeSeconds": 15.0,
      "content": "도입부에서 발음이 불명확합니다. 더 천천히 또박또박 말해보세요.",
      "type": "AI",
      "createdAt": "2026-04-09T12:00:00"
    }
  ]
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4008 | 영상을 찾을 수 없습니다 |

---

## 3. 영상 피드백 목록 조회

- **Method:** GET
- **URL:** `/api/videos/{videoId}/feedbacks`
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
> FeedbackResponse 배열 (MANUAL + AI 모두 포함)

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": [
    {
      "id": 1,
      "videoId": 1,
      "authorNickname": "멘토닉네임",
      "startTimeSeconds": 10.5,
      "endTimeSeconds": 25.0,
      "content": "말하는 속도를 줄이세요.",
      "type": "MANUAL",
      "createdAt": "2026-04-09T12:00:00"
    },
    {
      "id": 2,
      "videoId": 1,
      "authorNickname": "AI",
      "startTimeSeconds": 5.0,
      "endTimeSeconds": 15.0,
      "content": "발음이 불명확합니다.",
      "type": "AI",
      "createdAt": "2026-04-09T12:01:00"
    }
  ]
}
```

---

## 4. 피드백 단건 조회

- **Method:** GET
- **URL:** `/api/feedbacks/{feedbackId}`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| feedbackId | Long | O | 피드백 ID |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 1,
    "videoId": 1,
    "authorId": 2,
    "authorNickname": "멘토닉네임",
    "startTimeSeconds": 10.5,
    "endTimeSeconds": 25.0,
    "content": "말하는 속도를 줄이세요.",
    "type": "MANUAL",
    "createdAt": "2026-04-09T12:00:00"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4013 | 피드백을 찾을 수 없습니다 |
