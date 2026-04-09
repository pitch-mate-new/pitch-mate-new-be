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

    public void complete(Double speechRateWpm, Double silenceRatio, Integer fillerWordCount,
                         String fillerWords, Double speakingDurationSeconds) {
        this.status = AnalysisStatus.COMPLETED;
        this.speechRateWpm = speechRateWpm;
        this.silenceRatio = silenceRatio;
        this.fillerWordCount = fillerWordCount;
        this.fillerWords = fillerWords;
        this.speakingDurationSeconds = speakingDurationSeconds;
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
