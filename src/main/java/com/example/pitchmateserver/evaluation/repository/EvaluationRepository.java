package com.example.pitchmateserver.evaluation.repository;

import com.example.pitchmateserver.evaluation.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    @Query("SELECT e FROM Evaluation e JOIN FETCH e.scores s JOIN FETCH s.rubric WHERE e.video.id = :videoId ORDER BY e.createdAt DESC")
    List<Evaluation> findByVideoIdWithScores(@Param("videoId") Long videoId);
}
