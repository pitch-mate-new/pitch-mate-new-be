package com.example.pitchmateserver.video.entity;

import com.example.pitchmateserver.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "video_url", nullable = false)
    private String videoUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoType type;

    // TODO: 연습 유형 (발표/면접/스피치) - 추후 활성화
    // @Enumerated(EnumType.STRING)
    // @Column(name = "practice_type", nullable = false)
    // private PracticeType practiceType;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String title, String description) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
    }

    public enum VideoType {
        UPLOAD, RECORD
    }

    // TODO: 연습 유형 - 추후 활성화
    // public enum PracticeType {
    //     PRESENTATION, INTERVIEW, SPEECH
    // }
}
