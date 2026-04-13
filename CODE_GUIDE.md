# PitchMate Server - 코드 가이드

> 발표·면접 연습 영상 기반 AI 피드백 플랫폼  
> Spring Boot / Java 21 / Supabase PostgreSQL / Gemini AI

---

## 목차

1. [인증 (Auth)](#1-인증-auth)
2. [보안 / JWT (Security)](#2-보안--jwt-security)
3. [사용자 (User)](#3-사용자-user)
4. [영상 (Video)](#4-영상-video)
5. [AI 분석 (Analysis)](#5-ai-분석-analysis)
6. [피드백 (Feedback)](#6-피드백-feedback)
7. [평가 (Evaluation)](#7-평가-evaluation)
8. [루브릭 (Rubric)](#8-루브릭-rubric)
9. [히스토리 / 세션 (History)](#9-히스토리--세션-history)
10. [공통 (Common)](#10-공통-common)

---

## 1. 인증 (Auth)

### `AuthController`
- 경로: `auth/controller/AuthController.java`
- `/api/auth` 하위 모든 인증 API의 진입점
- 회원가입, 로그인, 로그아웃, 토큰 재발급, 이메일/닉네임 중복 확인 제공
- `@CurrentUser` 어노테이션으로 JWT에서 현재 유저 ID 추출

### `AuthService`
- 경로: `auth/service/AuthService.java`
- 실제 인증 비즈니스 로직 처리
- **회원가입**: 이메일·닉네임 중복 검사 → 비밀번호 BCrypt 암호화 → User 저장, role은 무조건 `"MENTEE"` 하드코딩
- **로그인**: 이메일로 유저 조회 → 비밀번호 일치 확인 → access/refresh 토큰 발급 → refresh 토큰 DB 저장
- **로그아웃**: refreshToken이 있으면 해당 토큰만 삭제, 없으면 해당 유저의 모든 refresh 토큰 삭제
- **토큰 재발급**: refresh 토큰 유효성 검사 → DB에서 존재 여부 + 만료 여부 확인 → 새 access/refresh 토큰 발급 후 DB 업데이트

### `RefreshToken` (Entity)
- 경로: `auth/entity/RefreshToken.java`
- DB 테이블: `refresh_tokens`
- 필드: `id`, `userId`, `token`(최대 500자), `expiresAt`
- `isExpired()`: 현재 시간이 `expiresAt`을 지났으면 true
- `updateToken()`: 재발급 시 토큰 값과 만료 시간을 갱신

### `RefreshTokenRepository`
- 경로: `auth/repository/RefreshTokenRepository.java`
- `findByUserId(Long)`: 유저의 refresh 토큰 조회 (1명 1토큰)
- `findByToken(String)`: 토큰 문자열로 조회
- `deleteByUserId(Long)`: 로그아웃 시 전체 삭제

### DTO 목록

| 파일 | 용도 |
|------|------|
| `LoginRequest` | 로그인 요청: `email`, `password` |
| `SignupRequest` | 회원가입 요청: `email`, `password`(8자↑), `nickname`(2~30자) |
| `SignupResponse` | 회원가입 응답: `userId`, `email`, `nickname` |
| `TokenResponse` | 로그인/재발급 응답: `accessToken`, `refreshToken`, `userId`, `nickname`, `role` |
| `LogoutRequest` | 로그아웃 요청: `refreshToken` (선택) |
| `ReissueRequest` | 토큰 재발급 요청: `refreshToken` |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| 4001 | 409 | 이미 사용 중인 이메일 |
| 4002 | 409 | 이미 사용 중인 닉네임 |
| 4003 | 401 | 이메일 또는 비밀번호 불일치 |
| 4004 | 401 | 유효하지 않은 refresh 토큰 |
| 4005 | 401 | 만료된 refresh 토큰 |

---

## 2. 보안 / JWT (Security)

### `JwtTokenProvider`
- 경로: `common/security/JwtTokenProvider.java`
- JWT 토큰 생성/검증 핵심 클래스
- `generateAccessToken(userId)`: access 토큰 생성, claim에 `type: "access"` 포함, 만료 30분
- `generateRefreshToken(userId)`: refresh 토큰 생성, claim에 `type: "refresh"` 포함, 만료 7일
- `getUserId(token)`: 토큰에서 userId 추출
- `validateToken(token)`: 서명/만료 등 유효성 검사, 실패 시 false (예외 던지지 않음)
- `isRefreshToken(token)`: claim의 type이 "refresh"인지 확인 → access 토큰을 refresh로 사용하는 것 방지

### `JwtAuthenticationFilter`
- 경로: `common/security/JwtAuthenticationFilter.java`
- 모든 요청에서 `Authorization: Bearer {token}` 헤더 파싱
- 유효한 access 토큰이면 `SecurityContextHolder`에 인증 정보 등록
- refresh 토큰으로 일반 API 호출하면 인증 실패 처리 (`isRefreshToken` 체크로 차단)
- 토큰이 없거나 유효하지 않아도 예외를 던지지 않고 다음 필터로 넘김 (인증 없는 요청은 SecurityConfig에서 처리)

### `SecurityConfig`
- 경로: `common/config/SecurityConfig.java`
- CSRF 비활성화, 세션 STATELESS
- **인증 없이 허용되는 경로**: `/api/auth/signup`, `/api/auth/login`, `/api/auth/reissue`, `/api/auth/check-email`, `/api/auth/check-nickname`, `/api/rubrics`, Swagger 관련
- **CORS 설정**: `cors.allowed-origins` 환경변수로 허용 출처 관리 (기본값: localhost:3000, localhost:5173)
- 허용 메서드: GET, POST, PUT, PATCH, DELETE, OPTIONS
- 자격증명(쿠키 등) 허용: `allowCredentials(true)`

### `CustomUserDetailsService`
- 경로: `common/security/CustomUserDetailsService.java`
- Spring Security의 `UserDetailsService` 구현체
- userId(String)를 받아서 DB에서 유저 조회 후 `UserPrincipal` 반환

### `UserPrincipal`
- 경로: `common/security/UserPrincipal.java`
- Spring Security의 `UserDetails` 구현체
- userId와 권한(role) 정보를 담아 SecurityContext에 저장됨

### `CurrentUser`
- 경로: `common/security/CurrentUser.java`
- 커스텀 어노테이션 (`@AuthenticationPrincipal` 래핑)
- 컨트롤러 파라미터에 `@CurrentUser Long userId`로 사용하면 JWT에서 자동으로 userId 주입

---

## 3. 사용자 (User)

### `UserController`
- 경로: `user/controller/UserController.java`
- `/api/users` 하위 사용자 정보 API

| API | 설명 |
|-----|------|
| `GET /api/users/me` | 내 정보 + 통계 (영상 수, 평가받은 영상 수, 평균 점수) |
| `GET /api/users/{userId}` | 다른 유저 프로필 조회 |
| `PUT /api/users/me` | 프로필 수정 (닉네임, 프로필이미지URL) |
| `DELETE /api/users/me` | 회원 탈퇴 |

### `UserService`
- 경로: `user/service/UserService.java`
- `getMyInfo()`: 유저 정보 + VideoRepository에서 통계 3개 조회 (총 영상 수, 평가된 영상 수, 평균 점수)
- `updateProfile()`: 닉네임 변경 시 중복 검사 후 업데이트, 이미지 URL도 같이 처리
- `deleteAccount()`: 유저 삭제 (연관 데이터 Cascade 처리됨)
- `findUser()`: 내부적으로 자주 사용되는 유저 조회 메서드, 없으면 `USER_NOT_FOUND` 예외

### `User` (Entity)
- 경로: `user/entity/User.java`
- DB 테이블: `users`
- 필드: `id`, `email`, `password`(암호화), `nickname`, `role`(`MENTEE` 고정), `profileImageUrl`, `createdAt`

### DTO 목록

| 파일 | 용도 |
|------|------|
| `UserResponse` | 유저 정보 응답: 기본 정보 + 통계 (videoCount, evaluatedCount, averageScore) |
| `UserStatsResponse` | 통계만 담는 내부용 DTO |
| `UpdateProfileRequest` | 프로필 수정 요청: `nickname`, `profileImageUrl` (둘 다 선택) |
| `UpdateProfileImageRequest` | 이미지만 수정할 때 사용 |
| `UpdateNicknameRequest` | 닉네임만 수정할 때 사용 |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| 4006 | 401 | 인증 필요 (토큰 없음/만료) |
| 4007 | 404 | 사용자를 찾을 수 없음 |

---

## 4. 영상 (Video)

### `VideoController`
- 경로: `video/controller/VideoController.java`
- `/api/videos` 하위 영상 CRUD API

| API | 설명 |
|-----|------|
| `POST /api/videos` | 영상 업로드 (multipart/form-data) |
| `GET /api/videos/my` | 내 영상 목록 (최신순) |
| `GET /api/videos/{videoId}` | 영상 상세 |
| `PUT /api/videos/{videoId}` | 영상 제목/설명 수정 |
| `DELETE /api/videos/{videoId}` | 영상 삭제 |

### `VideoService`
- 경로: `video/service/VideoService.java`
- **업로드 흐름**: 파일 저장(로컬 `uploads/videos/`) → Video 엔티티 저장 → Session 생성 → **AI 분석 비동기 시작** → **AI 평가 비동기 시작**
- 파일명은 `UUID_원본파일명` 형태로 저장 (중복 방지)
- 제목 미입력 시: RECORD 타입은 `"녹화 영상 {timestamp}"`, UPLOAD 타입은 원본 파일명 사용
- 수정/삭제 시 본인 소유 여부 검사 (`VIDEO_ACCESS_DENIED` 예외)
- `@Lazy` 의존성 사용: `AnalysisService`, `EvaluationService`, `SessionService` (순환 의존성 방지)

### `Video` (Entity)
- 경로: `video/entity/Video.java`
- DB 테이블: `videos`
- 필드: `id`, `user`, `title`, `description`, `videoUrl`(파일 경로), `type`, `createdAt`
- `VideoType` enum: `UPLOAD` (파일 업로드), `RECORD` (앱 녹화)

### DTO 목록

| 파일 | 용도 |
|------|------|
| `VideoResponse` | 영상 응답: `id`, `title`, `description`, `videoUrl`, `type`, `createdAt` |
| `VideoUpdateRequest` | 영상 수정 요청: `title`, `description` |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| 4008 | 404 | 영상을 찾을 수 없음 |
| 4009 | 403 | 본인 영상이 아님 |
| 4016 | 500 | 파일 저장 실패 |

---

## 5. AI 분석 (Analysis)

### `AnalysisController`
- 경로: `ai/controller/AnalysisController.java`
- `/api/videos/{videoId}/analysis`, `/api/analysis/{analysisId}` 하위 API

| API | 설명 |
|-----|------|
| `POST /api/videos/{videoId}/analysis` | 분석 요청 (영상 업로드 시 자동 호출되므로 수동 호출 불필요) |
| `GET /api/analysis/{analysisId}/status` | 분석 진행 상태 조회 |
| `GET /api/analysis/{analysisId}` | 분석 결과 조회 |
| `GET /api/videos/{videoId}/analysis` | 영상 ID로 분석 결과 조회 |
| `DELETE /api/analysis/{analysisId}` | 분석 삭제 |

### `AnalysisService`
- 경로: `ai/service/AnalysisService.java`
- `requestAnalysis()`: 이미 분석이 있으면 기존 결과 반환, 없으면 `PENDING` 상태로 Analysis 저장 후 비동기 분석 시작
- `runAnalysisAsync()`: `@Async` - Gemini에 영상 업로드 → 분석 요청 → 결과 저장
  - 상태 흐름: `PENDING` → `PROCESSING` → `COMPLETED` (실패 시 `FAILED`)
  - 실패해도 예외를 밖으로 던지지 않고 상태를 `FAILED`로 저장

### `GeminiService`
- 경로: `ai/service/GeminiService.java`
- Gemini API와의 모든 통신 담당
- **`uploadVideoFile()`**: 로컬 파일 → Gemini File API에 resumable 업로드 → fileUri 반환
  - 업로드 후 파일 상태가 `ACTIVE`가 될 때까지 3초 간격으로 최대 20번 폴링
- **`analyzeVideo(fileUri)`**: 말 속도(WPM), 침묵 비율, 필러워드 수/목록, 발화 시간, 전체 요약 분석
- **`generateFeedbacks(fileUri)`**: 개선이 필요한 구간별 피드백 3~5개 생성
- **`generateEvaluation(fileUri, rubricTitles, maxScore)`**: 루브릭 기준 점수 + 항목별 코멘트 + 종합 총평 생성
- 모든 메서드는 Gemini 실패 시 fallback 기본값 반환 (서비스 중단 방지)
- `extractJson()`: Gemini가 마크다운 코드블록으로 감싸서 응답할 때 JSON만 추출

### `Analysis` (Entity)
- 경로: `ai/entity/Analysis.java`
- DB 테이블: `analyses`
- 필드: `id`, `video`, `status`, `speechRateWpm`, `silenceRatio`, `fillerWordCount`, `fillerWords`(JSON 문자열), `speakingDurationSeconds`, `overallSummary`, `errorMessage`
- `AnalysisStatus` enum: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`
- `startProcessing()`, `complete()`, `fail()`: 상태 전환 메서드

### DTO

| 파일 | 용도 |
|------|------|
| `AnalysisResponse` | 분석 결과 응답: 상태 + 모든 분석 수치 |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| 4010 | 404 | 분석 결과를 찾을 수 없음 |
| 4011 | 409 | 이미 분석이 진행 중 |

---

## 6. 피드백 (Feedback)

### `FeedbackController`
- 경로: `feedback/controller/FeedbackController.java`
- `/api/videos/{videoId}/feedbacks` 하위 API

| API | 설명 |
|-----|------|
| `POST /api/videos/{videoId}/feedbacks/ai` | AI 피드백 생성 요청 |
| `GET /api/videos/{videoId}/feedbacks` | 해당 영상의 피드백 목록 조회 (시작시간 오름차순) |

### `FeedbackService`
- 경로: `feedback/service/FeedbackService.java`
- `generateAiFeedbacks()`: Gemini에 영상 업로드 → 구간별 피드백 3~5개 생성 → DB 저장
  - Gemini 실패 시 fallback 피드백 3개 (0~30초, 60~90초, 120~150초) 반환
- `getFeedbacksByVideo()`: 시작 시간 오름차순 정렬로 반환

### `Feedback` (Entity)
- 경로: `feedback/entity/Feedback.java`
- DB 테이블: `feedbacks`
- 필드: `id`, `video`, `author`(null = AI 피드백), `startTimeSeconds`, `endTimeSeconds`, `content`, `type`, `createdAt`
- `FeedbackType` enum: `AI`, `MANUAL` (현재 MANUAL은 미사용)

### DTO 목록

| 파일 | 용도 |
|------|------|
| `FeedbackRequest` | 피드백 생성 요청 (현재 미사용) |
| `FeedbackResponse` | 피드백 응답: id, 구간(start/end), content, type, createdAt |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| 4013 | 404 | 피드백을 찾을 수 없음 |

---

## 7. 평가 (Evaluation)

### `EvaluationController`
- 경로: `evaluation/controller/EvaluationController.java`
- `/api/videos/{videoId}/evaluations` 하위 API

| API | 설명 |
|-----|------|
| `POST /api/videos/{videoId}/evaluations/ai` | AI 평가 생성 요청 |
| `GET /api/videos/{videoId}/evaluations` | 해당 영상의 평가 목록 |

### `EvaluationService`
- 경로: `evaluation/service/EvaluationService.java`
- `generateAiEvaluationAsync()`: `@Async` - 영상 업로드 시 VideoService에서 자동 호출
- `generateAiEvaluation()`: Gemini에 영상 + 루브릭 10개 전달 → 각 항목별 점수/코멘트 + 종합 총평 생성
  - Evaluation 먼저 저장 → Gemini 호출 → EvaluationScore 저장 → total/max 업데이트
  - Gemini 실패 시 루브릭별 랜덤 점수 + 하드코딩된 fallback 코멘트 사용
  - `self` 주입(`@Lazy`): `@Async` + `@Transactional` 조합 때문에 self-invocation 문제 우회

### `Evaluation` (Entity)
- 경로: `evaluation/entity/Evaluation.java`
- DB 테이블: `evaluations`
- 필드: `id`, `video`, `evaluator`(null = AI 평가), `type`, `comment`(종합 총평), `totalScore`, `maxTotalScore`, `scores`(EvaluationScore 컬렉션)
- `EvaluationType` enum: `AI`, `MANUAL` (현재 MANUAL 미사용)

### `EvaluationScore` (Entity)
- 경로: `evaluation/entity/EvaluationScore.java`
- DB 테이블: `evaluation_scores`
- Evaluation과 Rubric을 연결하는 중간 엔티티
- 필드: `id`, `evaluation`, `rubric`, `score`, `comment`

### DTO 목록

| 파일 | 용도 |
|------|------|
| `EvaluationRequest` | 평가 생성 요청 (현재 미사용) |
| `EvaluationResponse` | 평가 응답: 총점, 최대점, 총평, 루브릭별 점수/코멘트 목록 |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| 4012 | 404 | 평가 결과를 찾을 수 없음 |

---

## 8. 루브릭 (Rubric)

### `RubricController`
- 경로: `rubric/controller/RubricController.java`
- `GET /api/rubrics`: 전체 루브릭 10개를 `displayOrder` 순서로 반환 (인증 불필요)

### `RubricService`
- 경로: `rubric/service/RubricService.java`
- `getAllRubrics()`: 표시 순서대로 정렬해서 반환
- `findRubric()`: 평가 생성 시 내부 조회용

### `DataInitializer`
- 경로: `rubric/DataInitializer.java`
- 서버 시작 시 루브릭이 없으면 자동으로 10개 삽입 (`@PostConstruct` 또는 `CommandLineRunner`)
- 루브릭 목록: 발음 정확성, 말하기 속도, 음성 변화, 시선 처리, 제스처, 자세 및 표정, 논리적 구성, 핵심전달력, 필러워드 빈도, 시간활용
- 각 항목 최대 점수: 10점

### `Rubric` (Entity)
- 경로: `rubric/entity/Rubric.java`
- DB 테이블: `rubrics`
- 필드: `id`, `title`, `description`, `maxScore`, `displayOrder`

### DTO

| 파일 | 용도 |
|------|------|
| `RubricResponse` | 루브릭 응답: id, title, description, maxScore, displayOrder |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| 4014 | 404 | 루브릭을 찾을 수 없음 |

---

## 9. 히스토리 / 세션 (History)

### `HistoryController`
- 경로: `session/controller/HistoryController.java`
- `/api/history` 하위 API

| API | 설명 |
|-----|------|
| `GET /api/history` | 내 히스토리 목록 (`?type=UPLOAD/RECORD`, `?limit=5` 선택) |
| `GET /api/history/{historyId}` | 히스토리 상세 (영상 + 피드백 + 평가 + 분석 한 번에) |
| `GET /api/history/compare` | 두 회차 비교 (`?sessionId1=&sessionId2=`) |

### `SessionService`
- 경로: `session/service/SessionService.java`
- `createSession()`: 영상 업로드 시 자동 호출, 해당 유저의 몇 번째 영상인지 카운트해서 `"{제목} (N회차)"` 형태로 세션 생성
- `getMyHistory()`: type 필터 + limit 적용, 각 세션마다 총점과 분석 상태를 함께 반환 (N+1 방지 위해 videoId 목록으로 한 번에 조회)
- `getSessionDetail()`: 세션 1개에 대해 영상·피드백·평가·분석을 각각 조회해서 묶어서 반환
- `compareSessions()`: 두 세션의 루브릭별 점수와 분석 수치(말 속도, 침묵 비율, 필러워드)를 비교해서 반환

### `Session` (Entity)
- 경로: `session/entity/Session.java`
- DB 테이블: `sessions`
- 필드: `id`, `user`, `video`, `title`, `sessionNumber`(회차), `createdAt`

### DTO 목록

| 파일 | 용도 |
|------|------|
| `SessionSummaryResponse` | 목록용 요약: 세션 기본 정보 + 총점 + 분석 상태 |
| `SessionDetailResponse` | 상세: 세션 + VideoResponse + 피드백 목록 + 평가 목록 + 분석 결과 |
| `SessionCompareResponse` | 비교: 두 세션 요약 + 루브릭별 점수 비교 + 분석 수치 비교 |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| 4015 | 404 | 히스토리(세션)를 찾을 수 없음 |

---

## 10. 공통 (Common)

### `ApiResponse`
- 경로: `common/response/ApiResponse.java`
- 모든 API 응답의 공통 래퍼
- 성공: `{ "code": 200, "status": 200, "message": "...", "data": {...} }`
- 실패: `{ "code": 4001, "status": 409, "message": "...", "data": null }`
- `ApiResponse.ok(data)`: 성공 응답
- `ApiResponse.error(code, status, message)`: 실패 응답

### `SuccessCode`
- 경로: `common/response/SuccessCode.java`
- 성공 응답 코드 enum (현재 `SUCCESS` 단일 코드 사용)

### `ErrorCode`
- 경로: `common/exception/ErrorCode.java`
- 모든 에러 코드를 enum으로 관리
- 각 에러마다 `code`(4자리 숫자), `status`(HTTP 상태), `message`(한국어 메시지) 포함
- 범위: Auth(4001~4006), User(4007), Video(4008~4009), Analysis(4010~4011), Evaluation(4012), Feedback(4013), Rubric(4014), History(4015), File(4016), Validation(4000), Server(5000)

### `BusinessException`
- 경로: `common/exception/BusinessException.java`
- 비즈니스 로직 예외의 기반 클래스
- `ErrorCode`를 받아서 생성: `throw new BusinessException(ErrorCode.USER_NOT_FOUND)`

### `GlobalExceptionHandler`
- 경로: `common/exception/GlobalExceptionHandler.java`
- `@RestControllerAdvice`로 전역 예외 처리
- `BusinessException` → ErrorCode의 HTTP 상태 + 메시지로 응답
- `MethodArgumentNotValidException` → 400 + 유효성 검사 실패 필드 메시지
- 나머지 모든 예외 → 500 서버 오류

### `AsyncConfig`
- 경로: `common/config/AsyncConfig.java`
- `@Async`가 동작하도록 스레드풀 설정
- AI 분석/평가가 비동기로 실행되는 데 필요

### `JacksonConfig`
- 경로: `common/config/JacksonConfig.java`
- JSON 직렬화 설정 (날짜 형식 등)

### `SwaggerConfig`
- 경로: `common/config/SwaggerConfig.java`
- Swagger UI 설정 (JWT Bearer 토큰 인증 버튼 활성화)
- 접속 주소: `https://pitch-mate-be-production.up.railway.app/swagger-ui/index.html`

---

## 전체 흐름 요약

```
영상 업로드 (POST /api/videos)
    ↓
파일 저장 (로컬 uploads/videos/)
    ↓
Video 엔티티 저장
    ↓
Session 생성 (N회차)
    ↓
AnalysisService.requestAnalysis() [비동기]   EvaluationService.generateAiEvaluationAsync() [비동기]
    ↓                                               ↓
GeminiService.uploadVideoFile()             GeminiService.uploadVideoFile()
    ↓                                               ↓
GeminiService.analyzeVideo()               GeminiService.generateEvaluation()
    ↓                                               ↓
Analysis 저장 (COMPLETED/FAILED)           Evaluation + EvaluationScore 저장
```
