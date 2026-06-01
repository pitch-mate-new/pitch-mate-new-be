package com.example.pitchmateserver.session.repository;

import com.example.pitchmateserver.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(s) + 1 FROM Session s WHERE s.user.id = :userId")
    Integer countNextSessionNumber(@Param("userId") Long userId);

    Optional<Session> findByVideoId(Long videoId);

    void deleteByVideoId(Long videoId);
}
