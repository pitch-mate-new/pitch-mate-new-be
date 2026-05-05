package com.example.pitchmateserver.connection.entity;

import com.example.pitchmateserver.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mentor_connections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mentee_id", "mentor_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MentorConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentee_id", nullable = false)
    private User mentee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status;

    @Column(length = 200)
    private String menteeIntro;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void accept() {
        this.status = ConnectionStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ConnectionStatus.REJECTED;
    }

    public enum ConnectionStatus {
        PENDING, ACCEPTED, REJECTED
    }
}
