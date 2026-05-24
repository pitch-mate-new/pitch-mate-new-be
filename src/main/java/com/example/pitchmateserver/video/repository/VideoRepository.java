package com.example.pitchmateserver.video.repository;

import com.example.pitchmateserver.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Video> findTop4ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);

    List<Video> findByRequestedMentorIdOrderByCreatedAtDesc(Long mentorId);

    @Query("SELECT AVG(e.totalScore * 100.0 / e.maxTotalScore) FROM Evaluation e WHERE e.video.user.id = :userId AND e.maxTotalScore > 0 AND e.type = 'AI'")
    Double findAverageScoreByUserId(@Param("userId") Long userId);
}
