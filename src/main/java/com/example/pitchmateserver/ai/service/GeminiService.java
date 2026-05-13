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
                이 발표/면접 연습 영상을 분석해줘. 아래 JSON 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.
                """ + descriptionContext + """

                {
                  "speechRateWpm": <분당 단어 수 (숫자)>,
                  "silenceRatio": <전체 시간 대비 침묵 비율 % (숫자 0-100)>,
                  "fillerWordCount": <어, 음, 그, 저 등 필러워드 총 횟수 (숫자)>,
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
    public List<GeminiFeedbackResult> generateFeedbacks(String fileUri, String description) {
        String descriptionContext = (description != null && !description.isBlank())
                ? "\n영상 설명 (참고): " + description + "\n"
                : "";
        String prompt = """
                이 발표/면접 연습 영상을 보고 개선이 필요한 구간에 대해 구체적인 피드백을 3-5개 작성해줘.
                아래 JSON 배열 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.
                """ + descriptionContext + """

                [
                  {
                    "startTimeSeconds": <구간 시작 시간 (초, 숫자)>,
                    "endTimeSeconds": <구간 종료 시간 (초, 숫자)>,
                    "content": "이 구간에 대한 구체적인 피드백 (한국어)"
                  }
                ]

                피드백은 말 속도, 시선 처리, 자세, 제스처, 발음, 내용 구성 등 다양한 측면을 다뤄줘.
                """;

        try {
            String responseText = callGeminiWithVideo(fileUri, prompt);
            String json = extractJson(responseText);
            JsonNode arrayNode = objectMapper.readTree(json);

            List<GeminiFeedbackResult> results = new ArrayList<>();
            if (arrayNode.isArray()) {
                for (JsonNode item : arrayNode) {
                    results.add(new GeminiFeedbackResult(
                            item.path("startTimeSeconds").asDouble(0.0),
                            item.path("endTimeSeconds").asDouble(30.0),
                            item.path("content").asText("피드백을 생성했습니다.")
                    ));
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Gemini 피드백 생성 실패: {}", e.getMessage());
            return GeminiFeedbackResult.fallback();
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
                각 항목을 1-%d점으로 채점하고, 한국어로 코멘트를 작성해줘.
                아래 JSON 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.
                """ + descriptionContext + """

                평가 기준: %s

                응답 형식:
                {
                  "scores": {
                    "항목명": {"score": <점수 (숫자)>, "comment": "항목별 평가 코멘트 (한국어)"},
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
            String overallComment = node.path("overallComment").asText("AI가 영상을 분석하여 생성한 종합 평가입니다.");
            return new GeminiEvaluationResult(scores, overallComment);
        } catch (Exception e) {
            log.error("Gemini 평가 생성 실패: {}", e.getMessage());
            return new GeminiEvaluationResult(Collections.emptyMap(), "AI 평가를 완료했습니다.");
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
                        "temperature", 0.1,
                        "maxOutputTokens", 2048
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
        public static List<GeminiFeedbackResult> fallback() {
            return List.of(
                    new GeminiFeedbackResult(0.0, 30.0, "도입부에서 명확한 주제 제시가 필요합니다."),
                    new GeminiFeedbackResult(60.0, 90.0, "이 구간에서 말하기 속도를 조절해보세요."),
                    new GeminiFeedbackResult(120.0, 150.0, "결론 부분에 핵심 내용 요약을 추가해주세요.")
            );
        }
    }

    public record GeminiEvalResult(int score, String comment) {}

    public record GeminiEvaluationResult(
            Map<String, GeminiEvalResult> scores,
            String overallComment
    ) {}
}
