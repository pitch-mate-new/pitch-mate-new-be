package com.example.pitchmateserver.connection.repository;

import com.example.pitchmateserver.connection.entity.MentorConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<MentorConnection, Long> {

    List<MentorConnection> findByMenteeIdOrderByCreatedAtDesc(Long menteeId);

    List<MentorConnection> findByMentorIdOrderByCreatedAtDesc(Long mentorId);

    List<MentorConnection> findByMentorIdAndStatus(Long mentorId, MentorConnection.ConnectionStatus status);

    List<MentorConnection> findByMenteeIdAndStatus(Long menteeId, MentorConnection.ConnectionStatus status);

    Optional<MentorConnection> findByMenteeIdAndMentorId(Long menteeId, Long mentorId);

    boolean existsByMenteeIdAndMentorId(Long menteeId, Long mentorId);

    boolean existsByMenteeIdAndMentorIdAndStatus(Long menteeId, Long mentorId, MentorConnection.ConnectionStatus status);

    long countByMentorIdAndStatus(Long mentorId, MentorConnection.ConnectionStatus status);

    long countByMenteeIdAndStatus(Long menteeId, MentorConnection.ConnectionStatus status);

    void deleteByMenteeId(Long menteeId);

    void deleteByMentorId(Long mentorId);
}
