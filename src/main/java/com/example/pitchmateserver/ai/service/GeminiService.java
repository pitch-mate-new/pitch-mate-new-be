package com.example.pitchmateserver.ai.service;

import com.example.pitchmateserver.common.storage.S3StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/**
 * Gemini 호출 결과를 그대로 반환하거나, 실패하면 예외를 던진다.
 * 실패를 기본값/임의값으로 대체하지 않는다 — 대체값은 실제 분석 결과와 구분할 수 없어 사용자를 오도한다.
 */
@Slf4j
@Service
public class GeminiService {


    // QR-REL-02: AI 응답이 60초를 넘기면 중단하고 해당 작업만 FAILED 처리
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    // 일시적 오류(429, 5xx, 연결 실패) 재시도: 최대 3회 시도, 시도 사이 대기시간
    private static final int MAX_ATTEMPTS = 3;
    private static final long[] RETRY_BACKOFF_MS = {5_000, 15_000};

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final S3StorageService storageService;

    // STP 6.3.1 결함 주입 테스트에서 WireMock 주소로 바꿀 수 있도록 설정값으로 둔다
    public GeminiService(ObjectMapper objectMapper, S3StorageService storageService,
                         @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        // read timeout은 응답 대기 시간에만 적용되어 대용량 영상 업로드(요청 본문 전송)는 끊지 않는다
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    /**
     * 영상 파일을 Gemini File API에 업로드하고 fileUri 반환
     * videoUrl: S3 공개 URL
     */
    public String uploadVideoFile(String videoUrl) throws IOException {
        // S3에서 영상 파일 다운로드
        byte[] fileBytes = storageService.downloadFile(videoUrl);
        long fileSize = fileBytes.length;
        String fileName = videoUrl.substring(videoUrl.lastIndexOf('/') + 1);
        String mimeType = "video/mp4";

        // Step 1: 업로드 세션 시작
        String initBody = objectMapper.writeValueAsString(
                Map.of("file", Map.of("display_name", fileName))
        );

        // 업로드 세션은 한 번 실패하면 재사용할 수 없으므로 세션 시작부터 통째로 재시도
        String uploadResponse = withRetry("파일 업로드", () -> {
            HttpHeaders initHeaders = restClient.post()
                    .uri("/upload/v1beta/files?key={key}&uploadType=resumable", apiKey)
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", String.valueOf(fileSize))
                    .header("X-Goog-Upload-Header-Content-Type", mimeType)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(initBody)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders();

            String uploadUrl = initHeaders.getFirst("x-goog-upload-url");
            if (uploadUrl == null) {
                throw new RuntimeException("Gemini File API 업로드 URL을 가져오지 못했습니다.");
            }

            // Step 2: 파일 업로드
            return restClient.put()
                    .uri(uploadUrl)
                    .header("X-Goog-Upload-Command", "upload, finalize")
                    .header("X-Goog-Upload-Offset", "0")
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(fileBytes)
                    .retrieve()
                    .body(String.class);
        });

        JsonNode uploadNode = objectMapper.readTree(uploadResponse);
        String fileUri = uploadNode.path("file").path("uri").asText();
        if (fileUri.isBlank()) {
            throw new RuntimeException("Gemini File API 응답에 file uri가 없음");
        }

        // Step 3: 파일 처리 완료 대기
        waitForFileReady(uploadNode.path("file").path("name").asText());

        return fileUri;
    }

    /**
     * Gemini File API 처리 완료 대기
     */
    private void waitForFileReady(String fileName) throws IOException {
        for (int i = 0; i < 20; i++) {
            sleep(3000);

            String statusResponse = withRetry("파일 상태 조회", () -> restClient.get()
                    .uri("/v1beta/" + fileName + "?key=" + apiKey)
                    .retrieve()
                    .body(String.class));

            JsonNode node = objectMapper.readTree(statusResponse);
            String state = node.path("state").asText();
            if ("ACTIVE".equals(state)) {
                log.info("Gemini 파일 처리 완료: {}", fileName);
                return;
            }
            if ("FAILED".equals(state)) {
                throw new RuntimeException("Gemini 파일 처리 실패: " + fileName);
            }
            log.info("Gemini 파일 처리 중... state={}", state);
        }
        throw new RuntimeException("Gemini 파일 처리 시간 초과");
    }

    /**
     * 영상 분석 - 말 속도, 침묵 비율, 필러워드 등
     */
    public GeminiAnalysisResult analyzeVideo(String fileUri, String description) throws IOException {
        String descriptionContext = (description != null && !description.isBlank())
                ? "\n\n영상 설명 (참고): " + description
                : "";
        String prompt = """
                이 발표/면접 연습 영상을 정밀하게 분석해줘. 아래 JSON 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.
                """ + descriptionContext + """

                분석 지침:
                - speechRateWpm: 실제 발화 구간에서 말한 단어 수를 발화 시간(분)으로 나눠 계산. 한국어 기준 자연스러운 속도는 분당 250~350음절.
                - silenceRatio: 전체 영상 길이 대비 1초 이상 발화 없는 구간의 합산 비율(%).
                - fillerWordCount: '어', '음', '그', '저', '아', '뭐', '이제', '그니까' 등 불필요한 간투사를 영상 전체에서 직접 세어 정확한 횟수 반환.
                - fillerWords: 실제로 감지된 필러워드만 목록으로 반환.
                - speakingDurationSeconds: 실제 발화(말소리가 있는) 구간의 총 초 수.
                - inappropriateExpressionCount: 영상에서 사용된 부적절한 표현(비속어, 욕설, 공격적이거나 비하하는 표현, 차별적 발언 등)의 총 횟수. 없으면 0.
                - inappropriateExpressions: 실제로 감지된 부적절한 표현을 짧은 문맥과 함께 목록으로 반환. 없으면 빈 배열.

                {
                  "speechRateWpm": <분당 단어 수 (숫자)>,
                  "silenceRatio": <전체 시간 대비 침묵 비율 % (숫자 0-100)>,
                  "fillerWordCount": <필러워드 총 횟수 (숫자)>,
                  "fillerWords": ["감지된", "필러워드", "목록"],
                  "speakingDurationSeconds": <실제 발화 시간 초 (숫자)>,
                  "inappropriateExpressionCount": <부적절한 표현 총 횟수 (숫자)>,
                  "inappropriateExpressions": ["감지된 부적절한 표현과 문맥"],
                  "overallSummary": "전반적인 발표 분석 요약 (한국어, 2-3문장)"
                }
                """;

        String responseText = callGeminiWithVideo(fileUri, prompt);
        JsonNode node = objectMapper.readTree(extractJson(responseText));

        // 응답에 값이 빠져 있으면 기본값으로 채우지 않고 실패 처리
        return new GeminiAnalysisResult(
                requireNumber(node, "speechRateWpm"),
                requireNumber(node, "silenceRatio"),
                (int) Math.round(requireNumber(node, "fillerWordCount")),
                requireArray(node, "fillerWords"),
                requireNumber(node, "speakingDurationSeconds"),
                (int) Math.round(requireNumber(node, "inappropriateExpressionCount")),
                requireArray(node, "inappropriateExpressions"),
                node.path("overallSummary").asText(null)
        );
    }

    /**
     * AI 구간 피드백 생성
     */
    public List<GeminiFeedbackResult> generateFeedbacks(String fileUri, String description, Integer durationSeconds) throws IOException {
        String descriptionContext = (description != null && !description.isBlank())
                ? "\n영상 설명 (참고): " + description + "\n"
                : "";
        String durationContext = (durationSeconds != null && durationSeconds > 0)
                ? "\n영상 총 길이: " + durationSeconds + "초. 타임스탬프는 반드시 0~" + durationSeconds + "초 범위 안에서만 지정해.\n"
                : "";
        String prompt = """
                이 발표/면접 연습 영상을 보고 개선이 필요한 구간에 대해 구체적인 피드백을 3-5개 작성해줘.
                아래 JSON 배열 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.
                """ + descriptionContext + durationContext + """

                피드백 작성 기준:
                - 각 피드백은 영상에서 실제로 관찰된 구체적인 행동이나 말을 근거로 작성할 것.
                - "잘하고 있습니다" 같은 칭찬이 아닌, 개선점 중심으로 작성할 것.
                - 피드백 내용은 "~초 구간에서 ~한 행동이 관찰됨. ~하게 개선하면 좋음" 형태로 구체적으로 작성할 것.
                - 말 속도, 시선 처리, 자세, 제스처, 발음, 필러워드, 내용 구성 등 다양한 측면을 골고루 다룰 것.

                [
                  {
                    "startTimeSeconds": <구간 시작 시간 (초, 숫자)>,
                    "endTimeSeconds": <구간 종료 시간 (초, 숫자)>,
                    "content": "이 구간에 대한 구체적인 피드백 (한국어)"
                  }
                ]
                """;

        String responseText = callGeminiWithVideo(fileUri, prompt);
        JsonNode arrayNode = objectMapper.readTree(extractJson(responseText));
        if (!arrayNode.isArray()) {
            throw new IllegalStateException("Gemini 피드백 응답이 배열이 아님");
        }

        // 형식이 깨진 항목은 임의 값으로 채우지 않고 버린다
        List<GeminiFeedbackResult> results = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            JsonNode startNode = item.get("startTimeSeconds");
            JsonNode endNode = item.get("endTimeSeconds");
            String content = item.path("content").asText("");
            if (startNode == null || !startNode.isNumber() || endNode == null || !endNode.isNumber() || content.isBlank()) {
                log.warn("Gemini 피드백 항목 형식 오류, 제외: {}", item);
                continue;
            }
            double start = startNode.asDouble();
            double end = endNode.asDouble();
            if (durationSeconds != null && durationSeconds > 0) {
                start = Math.min(start, durationSeconds);
                end = Math.min(end, durationSeconds);
            }
            if (start < 0 || end < start) {
                log.warn("Gemini 피드백 구간 오류, 제외: {}", item);
                continue;
            }
            results.add(new GeminiFeedbackResult(start, end, content));
        }
        if (results.isEmpty()) {
            throw new IllegalStateException("Gemini 피드백 응답에 유효한 항목이 없음");
        }
        return results;
    }

    /**
     * AI 루브릭 기반 평가 생성 (총평 포함)
     */
    public GeminiEvaluationResult generateEvaluation(String fileUri, List<String> rubricTitles, int maxScore, String description) {
        String rubricList = String.join(", ", rubricTitles);
        String descriptionContext = (description != null && !description.isBlank())
                ? "\n영상 설명 (참고): " + description + "\n"
                : "";
        String prompt = String.format("""
                이 발표/면접 연습 영상을 아래 평가 기준으로 채점해줘.
                아래 JSON 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.
                """ + descriptionContext + """

                [채점 기준 - 반드시 아래 기준에 따라 엄격하게 적용할 것]
                - 1~3점: 심각한 문제가 있어 즉각적인 개선이 필요한 수준
                - 4~5점: 미흡하며 반복 연습이 필요한 수준
                - 6~7점: 기본적인 수준, 평균적인 발표자
                - 8~9점: 우수한 수준, 효과적으로 전달됨
                - 10점 : 전문가 수준, 거의 완벽함

                [채점 원칙]
                - 반드시 영상에서 실제로 관찰한 구체적 근거를 바탕으로 채점할 것.
                - 근거 없이 높은 점수를 주지 말 것. 평균적인 발표자는 6~7점이 적절함.
                - 같은 영상을 다시 평가해도 동일한 점수가 나와야 함.
                - 각 항목의 comment는 관찰한 내용과 점수 이유를 1-2문장으로 명확히 작성할 것.

                평가 항목 (각 1-%d점): %s

                응답 형식:
                {
                  "scores": {
                    "항목명": {"score": <점수 (숫자)>, "comment": "관찰 근거와 점수 이유 (한국어)"},
                    ...
                  },
                  "overallComment": "전체 발표에 대한 종합 총평 (한국어, 3-5문장)"
                }
                """, maxScore, rubricList);

        try {
            String responseText = callGeminiWithVideo(fileUri, prompt);
            String json = extractJson(responseText);
            JsonNode node = objectMapper.readTree(json);

            // 점수가 빠진 항목은 기본 점수로 채우지 않는다 (EvaluationService에서 누락 시 실패 처리)
            Map<String, GeminiEvalResult> scores = new LinkedHashMap<>();
            node.path("scores").fields().forEachRemaining(entry -> {
                JsonNode val = entry.getValue();
                if (!val.path("score").isNumber()) {
                    log.warn("Gemini 평가 항목 점수 누락, 제외: {}", entry.getKey());
                    return;
                }
                String comment = val.path("comment").asText("");
                scores.put(entry.getKey(), new GeminiEvalResult(
                        val.path("score").asInt(),
                        comment.isBlank() ? null : comment
                ));
            });
            if (scores.isEmpty()) {
                throw new RuntimeException("Gemini 평가 응답에 scores가 없음");
            }
            String overallComment = node.path("overallComment").asText("");
            if (overallComment.isBlank()) {
                throw new RuntimeException("Gemini 평가 응답에 overallComment가 없음");
            }
            return new GeminiEvaluationResult(scores, overallComment);
        } catch (Exception e) {
            log.error("Gemini 평가 생성 실패: {}", e.getMessage());
            throw new RuntimeException("Gemini 평가 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Gemini API 호출 (영상 + 텍스트 프롬프트)
     */
    private String callGeminiWithVideo(String fileUri, String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("file_data", Map.of(
                                        "mime_type", "video/mp4",
                                        "file_uri", fileUri
                                )),
                                Map.of("text", prompt)
                        )
                )),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "maxOutputTokens", 8192
                )
        );

        String response = withRetry("generateContent", () -> restClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));

        try {
            JsonNode node = objectMapper.readTree(response);
            return node.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Gemini 응답 파싱 실패: " + response, e);
        }
    }

    /**
     * 응답 텍스트에서 JSON 부분만 추출
     */
    private String extractJson(String text) {
        text = text.trim();
        // 마크다운 코드블록 제거
        if (text.startsWith("```")) {
            text = text.replaceFirst("```[a-z]*\\n?", "").replaceAll("```$", "").trim();
        }
        // JSON 시작 위치 찾기
        int start = text.indexOf('{');
        int startArr = text.indexOf('[');
        if (startArr != -1 && (start == -1 || startArr < start)) {
            start = startArr;
        }
        if (start == -1) return text;
        return text.substring(start);
    }

    private static double requireNumber(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v != null && v.isNumber()) return v.asDouble();
        if (v != null && v.isTextual()) {
            try {
                return Double.parseDouble(v.asText().trim());
            } catch (NumberFormatException ignored) {
                // 아래에서 실패 처리
            }
        }
        throw new IllegalStateException("Gemini 응답에 숫자 필드 누락: " + field);
    }

    private static String requireArray(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || !v.isArray()) {
            throw new IllegalStateException("Gemini 응답에 배열 필드 누락: " + field);
        }
        return v.toString();
    }

    /**
     * 일시적 오류(429, 5xx, 연결 실패)만 재시도. 4xx 등 재시도해도 같은 결과인 오류는 즉시 던진다.
     * 응답 타임아웃은 재시도하지 않는다 — 60초씩 반복하면 PR-02(5분 내 COMPLETED/FAILED)를 넘길 수 있음.
     */
    private <T> T withRetry(String label, Supplier<T> call) {
        for (int attempt = 1; ; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                if (attempt >= MAX_ATTEMPTS || !isRetryable(e)) throw e;
                long delay = RETRY_BACKOFF_MS[attempt - 1];
                log.warn("Gemini {} 일시적 실패, {}ms 후 재시도 ({}/{}): {}", label, delay, attempt, MAX_ATTEMPTS, e.getMessage());
                sleep(delay);
            }
        }
    }

    private static boolean isRetryable(RuntimeException e) {
        if (e instanceof HttpStatusCodeException h) {
            return h.getStatusCode().value() == 429 || h.getStatusCode().is5xxServerError();
        }
        if (e instanceof ResourceAccessException) {
            return !(e.getCause() instanceof SocketTimeoutException);
        }
        return false;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini 대기 중 인터럽트", e);
        }
    }

    // ========== 결과 DTO ==========

    public record GeminiAnalysisResult(
            Double speechRateWpm,
            Double silenceRatio,
            Integer fillerWordCount,
            String fillerWords,
            Double speakingDurationSeconds,
            Integer inappropriateExpressionCount,
            String inappropriateExpressions,
            String overallSummary
    ) {}

    public record GeminiFeedbackResult(
            Double startTimeSeconds,
            Double endTimeSeconds,
            String content
    ) {}

    public record GeminiEvalResult(int score, String comment) {}

    public record GeminiEvaluationResult(
            Map<String, GeminiEvalResult> scores,
            String overallComment
    ) {}
}
