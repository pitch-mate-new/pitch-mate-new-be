# 루브릭 API 명세서

Base URL: `http://localhost:8080`

---

## 1. 루브릭 항목 전체 조회

- **Method:** GET
- **URL:** `/api/rubrics`
- **인증 필요:** X
- **설명:** 평가에 사용되는 루브릭 7개 항목을 조회합니다. 서버 시작 시 자동으로 주입됩니다.

### Header
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Content-Type | String | O | application/json |

### Request Example
```
GET /api/rubrics
```

### Success Response (200)
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 루브릭 ID |
| title | String | 항목명 |
| description | String | 항목 설명 |
| maxScore | Integer | 최대 점수 |
| displayOrder | Integer | 표시 순서 |

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "result": [
    { "id": 1, "title": "내용 구성", "description": "발표 내용의 논리성과 구성력", "maxScore": 10, "displayOrder": 1 },
    { "id": 2, "title": "전달력", "description": "청중에게 내용을 효과적으로 전달하는 능력", "maxScore": 10, "displayOrder": 2 },
    { "id": 3, "title": "말하기 속도", "description": "적절한 말하기 속도 유지", "maxScore": 10, "displayOrder": 3 },
    { "id": 4, "title": "발음·억양", "description": "명확한 발음과 자연스러운 억양", "maxScore": 10, "displayOrder": 4 },
    { "id": 5, "title": "시선 처리", "description": "청중과의 눈 맞춤 및 시선 분배", "maxScore": 10, "displayOrder": 5 },
    { "id": 6, "title": "자세·제스처", "description": "발표 중 자세와 제스처의 자연스러움", "maxScore": 10, "displayOrder": 6 },
    { "id": 7, "title": "시간 관리", "description": "주어진 시간 내 발표 완료 여부", "maxScore": 10, "displayOrder": 7 }
  ]
}
```
