package com.example.pitchmateserver.ai.dto;

import com.example.pitchmateserver.ai.entity.Analysis;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnalysisResponse {

    private Long analysisId;
    private Long videoId;
    @Schema(description = "분석 상태", allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"})
    private String status;

    // 분석 결과 (COMPLETED 상태일 때만 채워짐)
    private Double speechRateWpm;
    private Double silenceRatio;
    private Integer fillerWordCount;
    @JsonRawValue
    private String fillerWords;
    private Double speakingDurationSeconds;

    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AnalysisResponse from(Analysis analysis) {
        return AnalysisResponse.builder()
                .analysisId(analysis.getId())
                .videoId(analysis.getVideo().getId())
                .status(analysis.getStatus().name())
                .speechRateWpm(analysis.getSpeechRateWpm())
                .silenceRatio(analysis.getSilenceRatio())
                .fillerWordCount(analysis.getFillerWordCount())
                .fillerWords(analysis.getFillerWords())
                .speakingDurationSeconds(analysis.getSpeakingDurationSeconds())
                .errorMessage(analysis.getErrorMessage())
                .createdAt(analysis.getCreatedAt())
                .updatedAt(analysis.getUpdatedAt())
                .build();
    }
}
