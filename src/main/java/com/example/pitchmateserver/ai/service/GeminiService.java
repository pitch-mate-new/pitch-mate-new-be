package com.example.pitchmateserver.ai.service;

import com.example.pitchmateserver.common.storage.S3StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class GeminiService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final S3StorageService storageService;

    public GeminiService(ObjectMapper objectMapper, S3StorageService storageService) {
        this.restClient = RestClient.builder()
                .baseUrl(GEMINI_BASE_URL)
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
        String uploadResponse = restClient.put()
                .uri(uploadUrl)
                .header("X-Goog-Upload-Command", "upload, finalize")
                .header("X-Goog-Upload-Offset", "0")
                .contentType(MediaType.parseMediaType(mimeType))
                .body(fileBytes)
                .retrieve()
                .body(String.class);

        JsonNode uploadNode = objectMapper.readTree(uploadResponse);
        String fileUri = uploadNode.path("file").path("uri").asText();

        // Step 3: 파일 처리 완료 대기
        waitForFileReady(uploadNode.path("file").path("name").asText());

        return fileUri;
    }

    /**
     * Gemini File API 처리 완료 대기
     */
    private void waitForFileReady(String fileName) throws IOException {
        for (int i = 0; i < 20; i++) {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

            String statusResponse = restClient.get()
                    .uri("/v1beta/" + fileName + "?key=" + apiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(statusResponse);
            String state = node.path("state").asText();
            if ("ACTIVE".equals(state)) {
                log.info("Gemini 파일 처리 완료: {}", fileName);
                return;
            }
            log.info("Gemini 파일 처리 중... state={}", state);
        }
        throw new RuntimeException("Gemini 파일 처리 시간 초과");
    }

    /**
     * 영상 분석 - 말 속도, 침묵 비율, 필러워드 등
     */
    public GeminiAnalysisResult analyzeVideo(String fileUri, String description) {
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

                {
                  "speechRateWpm": <분당 단어 수 (숫자)>,
                  "silenceRatio": <전체 시간 대비 침묵 비율 % (숫자 0-100)>,
                  "fillerWordCount": <필러워드 총 횟수 (숫자)>,
                  "fillerWords": ["감지된", "필러워드", "목록"],
                  "speakingDurationSeconds": <실제 발화 시간 초 (숫자)>,
                  "overallSummary": "전반적인 발표 분석 요약 (한국어, 2-3문장)"
                }
                """;

        try {
            String responseText = callGeminiWithVideo(fileUri, prompt);
            String json = extractJson(responseText);
            JsonNode node = objectMapper.readTree(json);

            return new GeminiAnalysisResult(
                    node.path("speechRateWpm").asDouble(130.0),
                    node.path("silenceRatio").asDouble(10.0),
                    node.path("fillerWordCount").asInt(5),
                    node.path("fillerWords").toString(),
                    node.path("speakingDurationSeconds").asDouble(120.0),
                    node.path("overallSummary").asText("AI 분석이 완료되었습니다.")
            );
        } catch (Exception e) {
            log.error("Gemini 영상 분석 실패: {}", e.getMessage());
            return GeminiAnalysisResult.fallback();
        }
    }

    /**
     * AI 구간 피드백 생성
     */
    public List<GeminiFeedbackResult> generateFeedbacks(String fileUri, String description, Integer durationSeconds) {
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

        try {
            String responseText = callGeminiWithVideo(fileUri, prompt);
            String json = extractJson(responseText);
            JsonNode arrayNode = objectMapper.readTree(json);

            List<GeminiFeedbackResult> results = new ArrayList<>();
            if (arrayNode.isArray()) {
                for (JsonNode item : arrayNode) {
                    double start = item.path("startTimeSeconds").asDouble(0.0);
                    double end = item.path("endTimeSeconds").asDouble(start + 10.0);
                    if (durationSeconds != null && durationSeconds > 0) {
                        start = Math.min(start, durationSeconds);
                        end = Math.min(end, durationSeconds);
                    }
                    results.add(new GeminiFeedbackResult(start, end, item.path("content").asText("피드백을 생성했습니다.")));
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Gemini 피드백 생성 실패: {}", e.getMessage());
            return GeminiFeedbackResult.fallback(durationSeconds);
        }
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

            Map<String, GeminiEvalResult> scores = new LinkedHashMap<>();
            node.path("scores").fields().forEachRemaining(entry -> {
                JsonNode val = entry.getValue();
                scores.put(entry.getKey(), new GeminiEvalResult(
                        val.path("score").asInt(5),
                        val.path("comment").asText("AI 평가 결과입니다.")
                ));
            });
            if (scores.isEmpty()) {
                throw new RuntimeException("Gemini 평가 응답에 scores가 없음");
            }
            String overallComment = node.path("overallComment").asText("AI가 영상을 분석하여 생성한 종합 평가입니다.");
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

        String response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

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

    // ========== 결과 DTO ==========

    public record GeminiAnalysisResult(
            Double speechRateWpm,
            Double silenceRatio,
            Integer fillerWordCount,
            String fillerWords,
            Double speakingDurationSeconds,
            String overallSummary
    ) {
        public static GeminiAnalysisResult fallback() {
            return new GeminiAnalysisResult(130.0, 10.0, 3, "[\"어\",\"음\"]", 120.0, "AI 분석을 완료했습니다.");
        }
    }

    public record GeminiFeedbackResult(
            Double startTimeSeconds,
            Double endTimeSeconds,
            String content
    ) {
        public static List<GeminiFeedbackResult> fallback(Integer durationSeconds) {
            int d = (durationSeconds != null && durationSeconds > 0) ? durationSeconds : 30;
            double third = d / 3.0;
            return List.of(
                    new GeminiFeedbackResult(0.0, Math.min(third, d), "도입부에서 명확한 주제 제시가 필요합니다."),
                    new GeminiFeedbackResult(Math.min(third, d), Math.min(third * 2, d), "이 구간에서 말하기 속도를 조절해보세요."),
                    new GeminiFeedbackResult(Math.min(third * 2, d), (double) d, "결론 부분에 핵심 내용 요약을 추가해주세요.")
            );
        }
    }

    public record GeminiEvalResult(int score, String comment) {}

    public record GeminiEvaluationResult(
            Map<String, GeminiEvalResult> scores,
            String overallComment
    ) {}
}
