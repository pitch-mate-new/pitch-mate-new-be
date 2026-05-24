package com.example.pitchmateserver.evaluation.entity;

import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.video.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    // null이면 AI 평가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id")
    private User evaluator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationType type;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "max_total_score")
    private Integer maxTotalScore;

    @Column(length = 2000)
    private String comment;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationScore> scores = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void updateTotals(int totalScore, int maxTotalScore) {
        this.totalScore = totalScore;
        this.maxTotalScore = maxTotalScore;
    }

    public int getNormalizedScore() {
        return (maxTotalScore != null && maxTotalScore > 0)
                ? (int) Math.round(totalScore * 100.0 / maxTotalScore)
                : 0;
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }

    public enum EvaluationType {
        MANUAL,  // 멘토가 직접 평가
        AI       // AI 자동 평가
    }
}
