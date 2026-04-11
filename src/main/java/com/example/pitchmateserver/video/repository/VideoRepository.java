package com.example.pitchmateserver.video.repository;

import com.example.pitchmateserver.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);

    @Query("SELECT COUNT(DISTINCT e.video.id) FROM Evaluation e WHERE e.video.user.id = :userId")
    long countEvaluatedVideosByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(e.totalScore) FROM Evaluation e WHERE e.video.user.id = :userId AND e.totalScore IS NOT NULL")
    Double findAverageScoreByUserId(@Param("userId") Long userId);
}
