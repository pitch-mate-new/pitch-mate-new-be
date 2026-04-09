# AI 분석 API 명세서

Base URL: `http://localhost:8080`

---

## 1. AI 분석 요청

- **Method:** POST
- **URL:** `/api/videos/{videoId}/analysis`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| videoId | Long | O | 영상 ID |

### Success Response (202 Accepted)
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 분석 ID |
| videoId | Long | 영상 ID |
| status | String | 분석 상태 (PENDING / IN_PROGRESS / COMPLETED / FAILED) |
| createdAt | DateTime | 요청 일시 |

```json
{
  "code": 1000,
  "status": 202,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 1,
    "videoId": 1,
    "status": "PENDING",
    "createdAt": "2026-04-09T12:00:00"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4008 | 영상을 찾을 수 없습니다 |
| 409 | 4011 | 이미 분석이 진행 중입니다 |

---

## 2. 분석 상태 조회

- **Method:** GET
- **URL:** `/api/analysis/{analysisId}/status`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| analysisId | Long | O | 분석 ID |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "analysisId": 1,
    "videoId": 1,
    "status": "COMPLETED"
  }
}
```

---

## 3. 분석 결과 전체 조회

- **Method:** GET
- **URL:** `/api/analysis/{analysisId}`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| analysisId | Long | O | 분석 ID |

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 분석 ID |
| videoId | Long | 영상 ID |
| status | String | 분석 상태 |
| speechRateWpm | Double | 말 속도 (단어/분) |
| silenceRatio | Double | 침묵 비율 (0.0~1.0) |
| fillerWordCount | Integer | 필러워드 총 개수 |
| fillerWords | String | 필러워드 목록 (쉼표 구분) |
| speakingDurationSeconds | Double | 실제 발화 시간 (초) |
| createdAt | DateTime | 분석 요청 일시 |
| updatedAt | DateTime | 분석 완료 일시 |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 1,
    "videoId": 1,
    "status": "COMPLETED",
    "speechRateWpm": 120.5,
    "silenceRatio": 0.15,
    "fillerWordCount": 8,
    "fillerWords": "음,어,그,저",
    "speakingDurationSeconds": 185.3,
    "createdAt": "2026-04-09T12:00:00",
    "updatedAt": "2026-04-09T12:01:30"
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4010 | 분석 결과를 찾을 수 없습니다 |

---

## 4. 영상 기준 분석 결과 조회

- **Method:** GET
- **URL:** `/api/videos/{videoId}/analysis`
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
> 분석 결과 전체 조회와 동일한 구조

---

## 5. 말 속도 분석 결과 조회

- **Method:** GET
- **URL:** `/api/analysis/{analysisId}/speech-rate`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| analysisId | Long | O | 분석 ID |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "analysisId": 1,
    "wpm": 120.5,
    "speakingDurationSeconds": 185.3,
    "status": "COMPLETED"
  }
}
```

---

## 6. 침묵 구간 분석 결과 조회

- **Method:** GET
- **URL:** `/api/analysis/{analysisId}/silence`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| analysisId | Long | O | 분석 ID |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "analysisId": 1,
    "silenceRatio": 0.15,
    "status": "COMPLETED"
  }
}
```

---

## 7. 필러워드 분석 결과 조회

- **Method:** GET
- **URL:** `/api/analysis/{analysisId}/filler-words`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| analysisId | Long | O | 분석 ID |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "analysisId": 1,
    "fillerWordCount": 8,
    "fillerWords": "음,어,그,저",
    "status": "COMPLETED"
  }
}
```

---

## 8. 분석 결과 삭제

- **Method:** DELETE
- **URL:** `/api/analysis/{analysisId}`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| analysisId | Long | O | 분석 ID |

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "분석 결과가 삭제되었습니다.",
  "result": null
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4010 | 분석 결과를 찾을 수 없습니다 |
