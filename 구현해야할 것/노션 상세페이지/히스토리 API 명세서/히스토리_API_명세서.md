# 히스토리 API 명세서

Base URL: `http://localhost:8080`

> **히스토리(Session)란?** 영상 1개 업로드 = 회차 1개 자동 생성. 회차별 영상 + 피드백 + 평가 + 분석 묶음.

---

## 1. 히스토리 목록 조회

- **Method:** GET
- **URL:** `/api/history`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 히스토리(Session) ID |
| videoId | Long | 영상 ID |
| videoTitle | String | 영상 제목 |
| videoThumbnailUrl | String | 영상 썸네일 URL |
| videoType | String | 영상 타입 (UPLOAD / RECORD) |
| title | String | 회차 제목 |
| sessionNumber | Integer | 회차 번호 |
| createdAt | DateTime | 생성 일시 |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": [
    {
      "id": 1,
      "videoId": 1,
      "videoTitle": "발표 연습 1회",
      "videoThumbnailUrl": null,
      "videoType": "UPLOAD",
      "title": "발표 연습 1회",
      "sessionNumber": 1,
      "createdAt": "2026-04-09T12:00:00"
    }
  ]
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |

---

## 2. 히스토리 상세 조회

- **Method:** GET
- **URL:** `/api/history/{historyId}`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Path Variable
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| historyId | Long | O | 히스토리(Session) ID |

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 히스토리 ID |
| title | String | 회차 제목 |
| sessionNumber | Integer | 회차 번호 |
| createdAt | DateTime | 생성 일시 |
| video | Object | 영상 정보 |
| feedbacks | Array | 피드백 목록 |
| evaluations | Array | 평가 목록 |
| analysis | Object | AI 분석 결과 (없으면 null) |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "id": 1,
    "title": "발표 연습 1회",
    "sessionNumber": 1,
    "createdAt": "2026-04-09T12:00:00",
    "video": {
      "id": 1,
      "title": "발표 연습 1회",
      "videoUrl": "/videos/uuid_sample.mp4",
      "type": "UPLOAD",
      "createdAt": "2026-04-09T12:00:00"
    },
    "feedbacks": [
      {
        "id": 1,
        "authorNickname": "멘토닉네임",
        "startTimeSeconds": 10.5,
        "endTimeSeconds": 25.0,
        "content": "말하는 속도를 줄이세요.",
        "type": "MANUAL"
      }
    ],
    "evaluations": [
      {
        "id": 1,
        "evaluatorNickname": "멘토닉네임",
        "type": "MANUAL",
        "totalScore": 51,
        "maxTotalScore": 70
      }
    ],
    "analysis": {
      "id": 1,
      "status": "COMPLETED",
      "speechRateWpm": 120.5,
      "silenceRatio": 0.15,
      "fillerWordCount": 8
    }
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4015 | 히스토리를 찾을 수 없습니다 |

---

## 3. 두 회차 비교 분석

- **Method:** GET
- **URL:** `/api/history/compare`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Query Parameter
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| sessionId1 | Long | O | 첫 번째 회차 ID |
| sessionId2 | Long | O | 두 번째 회차 ID |

### Request Example
```
GET /api/history/compare?sessionId1=1&sessionId2=2
```

### Success Response (200)
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": {
    "session1": { "id": 1, "title": "발표 연습 1회", "sessionNumber": 1, "videoType": "UPLOAD" },
    "session2": { "id": 2, "title": "발표 연습 2회", "sessionNumber": 2, "videoType": "RECORD" },
    "evaluationScores": {
      "session1TotalScore": 51,
      "session2TotalScore": 58,
      "session1MaxScore": 70,
      "session2MaxScore": 70,
      "rubricComparisons": [
        { "rubricId": 1, "rubricTitle": "내용 구성", "session1Score": 8, "session2Score": 9, "maxScore": 10 }
      ]
    },
    "analysisData": {
      "session1SpeechRateWpm": 120.5,
      "session2SpeechRateWpm": 110.3,
      "session1SilenceRatio": 0.15,
      "session2SilenceRatio": 0.10,
      "session1FillerWordCount": 8,
      "session2FillerWordCount": 5
    }
  }
}
```

### Error Response
| 상태코드 | 코드 | 설명 |
|----------|------|------|
| 401 | 4006 | 인증이 필요합니다 |
| 404 | 4015 | 히스토리를 찾을 수 없습니다 |

---

## 4. 연습 유형별 히스토리 조회

- **Method:** GET
- **URL:** `/api/history/my/filter`
- **인증 필요:** O

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Query Parameter
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| type | String | X | 영상 타입 (UPLOAD / RECORD), 없으면 전체 조회 |

### Request Example
```
GET /api/history/my/filter?type=UPLOAD
```

### Success Response (200)
> SessionSummaryResponse 배열 (히스토리 목록 조회와 동일한 구조)

---

## 5. 최근 연습 요약 목록 조회

- **Method:** GET
- **URL:** `/api/history/my/recent-summary`
- **인증 필요:** O
- **설명:** 최근 5회차 히스토리 요약을 조회합니다.

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | Bearer {accessToken} |

### Success Response (200)
> SessionSummaryResponse 배열 (최대 5개)

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": [
    {
      "id": 5,
      "videoTitle": "발표 연습 5회",
      "videoType": "UPLOAD",
      "sessionNumber": 5,
      "createdAt": "2026-04-09T17:00:00"
    }
  ]
}
```
