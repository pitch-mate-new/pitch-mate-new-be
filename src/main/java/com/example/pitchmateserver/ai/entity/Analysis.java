package com.example.pitchmateserver.ai.entity;

import com.example.pitchmateserver.video.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false, unique = true)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    // 말 속도 (분당 단어 수)
    @Column(name = "speech_rate_wpm")
    private Double speechRateWpm;

    // 침묵 구간 비율 (%)
    @Column(name = "silence_ratio")
    private Double silenceRatio;

    // 필러워드 횟수 (어, 음, 그 등)
    @Column(name = "filler_word_count")
    private Integer fillerWordCount;

    // 필러워드 목록 (JSON 형태로 저장)
    @Column(name = "filler_words", length = 1000)
    private String fillerWords;

    // 발화 시간 (초)
    @Column(name = "speaking_duration_seconds")
    private Double speakingDurationSeconds;

    // 부적절한 표현(비속어, 공격적/차별적 언어 등) 탐지 횟수
    @Column(name = "inappropriate_expression_count")
    private Integer inappropriateExpressionCount;

    // 탐지된 부적절 표현 목록 (JSON 형태로 저장)
    @Column(name = "inappropriate_expressions", length = 1000)
    private String inappropriateExpressions;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void startProcessing() {
        this.status = AnalysisStatus.IN_PROGRESS;
    }

    // 지표만 기록하고 상태는 IN_PROGRESS 유지 — 평가/피드백까지 모두 끝나야 complete()
    public void recordMetrics(Double speechRateWpm, Double silenceRatio, Integer fillerWordCount,
                              String fillerWords, Double speakingDurationSeconds,
                              Integer inappropriateExpressionCount, String inappropriateExpressions) {
        this.speechRateWpm = speechRateWpm;
        this.silenceRatio = silenceRatio;
        this.fillerWordCount = fillerWordCount;
        this.fillerWords = fillerWords;
        this.speakingDurationSeconds = speakingDurationSeconds;
        this.inappropriateExpressionCount = inappropriateExpressionCount;
        this.inappropriateExpressions = inappropriateExpressions;
    }

    // 단계 사이 진행 신호 — 오래 갱신되지 않은 작업은 고아 작업으로 정리된다 (QR-REL-04)
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = AnalysisStatus.COMPLETED;
    }

    public void fail(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public enum AnalysisStatus {
        PENDING,      // 대기 중
        IN_PROGRESS,  // 분석 중
        COMPLETED,    // 완료
        FAILED        // 실패
    }
}
