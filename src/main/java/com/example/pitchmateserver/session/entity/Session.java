package com.example.pitchmateserver.session.entity;

import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.video.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 연습 히스토리 회차 단위.
 * 영상 1개 업로드 = 1회차 세션 자동 생성.
 */
@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    // "면접 연습 1회차" 등
    @Column(nullable = false)
    private String title;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
