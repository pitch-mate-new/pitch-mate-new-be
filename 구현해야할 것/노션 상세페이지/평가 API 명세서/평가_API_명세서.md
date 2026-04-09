# 평가 API 명세서

Base URL: `http://localhost:8080`

---

## 1. 멘토 직접 루브릭 평가 작성

- **Method:** POST
- **URL:** `/api/videos/{videoId}/evaluations`
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
| comment | String | X | 전체 총평 |
| scores | Array | O | 루브릭별 점수 목록 |
| scores[].rubricId | Long | O | 루브릭 ID |
| scores[].score | Integer | O | 점수 |
| scores[].comment | String | X | 해당 항목 코멘트 |

```json
{
  "comment": "전반적으로 발표 준비가 잘 되어 있습니다.",
  "scores": [
    { "rubricId": 1, "score": 8, "comment": "내용 구성이 논리적입니다." },
    { "rubricId": 2, "score": 7, "comment": "전달력이 좋습니다." },
    { "rubricId": 3, "score": 6, "comment": "말하기 속도가 약간 빠릅니다." },
    { "rubricId": 4, "score": 8, "comment": "발음이 명확합니다." },
    { "rubricId": 5, "score": 7, "comment": "시선 처리가 자연스럽습니다." },
    { "rubricId": 6, "score": 6, "comment": "제스처가 조금 부자연스럽습니다." },
    { "rubricId": 7, "score": 9, "comment": "시간 관리가 훌륭합니다." }
  ]
}
```

### Success Response (201)
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 평가 ID |
| videoId | Long | 영상 ID |
| evaluatorId | Long | 평가자 ID |
| evaluatorNickname | String | 평가자 닉네임 |
| type | String | 평가 타입 (MANUAL) |
| totalScore | Integer | 총점 |
| maxTotalScore | Integer | 최대 총점 |
| comment | String | 전체 총평 |
| scores | Array | 루브릭별 점수 목록 |
| createdAt | DateTime | 평가 일시 |

```json
{
  "code": 1000,
  "status": 201,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 1,
    "videoId": 1,
    "evaluatorId": 2,
    "evaluatorNickname": "멘토닉네임",
    "type": "MANUAL",
    "totalScore": 51,
    "maxTotalScore": 70,
    "comment": "전반적으로 발표 준비가 잘 되어 있습니다.",
    "scores": [
      {
        "rubricId": 1,
        "rubricTitle": "내용 구성",
        "score": 8,
        "maxScore": 10,
        "comment": "내용 구성이 논리적입니다."
      }
    ],
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
| 404 | 4014 | 루브릭을 찾을 수 없습니다 |

---

## 2. AI 루브릭 기반 평가 생성

- **Method:** POST
- **URL:** `/api/videos/{videoId}/evaluations/ai`
- **인증 필요:** O
- **설명:** Gemini AI가 영상을 분석하여 7개 루브릭 항목을 자동 평가합니다.

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| videoId | Long | O | 영상 ID |

### Success Response (201)
> EvaluationResponse (type = "AI")

```json
{
  "code": 1000,
  "status": 201,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 2,
    "videoId": 1,
    "evaluatorId": null,
    "evaluatorNickname": "AI",
    "type": "AI",
    "totalScore": 47,
    "maxTotalScore": 70,
    "comment": "AI 분석 결과입니다.",
    "scores": [...],
    "createdAt": "2026-04-09T12:00:00"
  }
}
```

---

## 3. 영상 평가 목록 조회

- **Method:** GET
- **URL:** `/api/videos/{videoId}/evaluations`
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
  "result": [
    {
      "id": 1,
      "type": "MANUAL",
      "evaluatorNickname": "멘토닉네임",
      "totalScore": 51,
      "maxTotalScore": 70,
      "createdAt": "2026-04-09T12:00:00"
    },
    {
      "id": 2,
      "type": "AI",
      "evaluatorNickname": "AI",
      "totalScore": 47,
      "maxTotalScore": 70,
      "createdAt": "2026-04-09T12:01:00"
    }
  ]
}
```

---

## 4. 평가 단건 조회

- **Method:** GET
- **URL:** `/api/evaluations/{evaluationId}`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| evaluationId | Long | O | 평가 ID |

### Success Response (200)
> 루브릭별 scores 포함 전체 EvaluationResponse

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4012 | 평가 결과를 찾을 수 없습니다 |

---

## 비고

### 루브릭 7개 항목
| ID | 항목 | 최대 점수 |
|----|------|-----------|
| 1 | 내용 구성 | 10 |
| 2 | 전달력 | 10 |
| 3 | 말하기 속도 | 10 |
| 4 | 발음·억양 | 10 |
| 5 | 시선 처리 | 10 |
| 6 | 자세·제스처 | 10 |
| 7 | 시간 관리 | 10 |
