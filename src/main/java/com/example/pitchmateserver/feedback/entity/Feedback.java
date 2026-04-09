package com.example.pitchmateserver.feedback.entity;

import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.video.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    // null이면 AI 생성 피드백
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    // 구간 시작 시간 (초)
    @Column(name = "start_time_seconds")
    private Double startTimeSeconds;

    // 구간 종료 시간 (초)
    @Column(name = "end_time_seconds")
    private Double endTimeSeconds;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum FeedbackType {
        MANUAL,  // 멘토가 직접 작성
        AI       // AI 자동 생성
    }
}
