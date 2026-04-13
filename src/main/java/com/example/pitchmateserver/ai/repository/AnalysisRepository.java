package com.example.pitchmateserver.ai.repository;

import com.example.pitchmateserver.ai.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    Optional<Analysis> findByVideoId(Long videoId);
    boolean existsByVideoId(Long videoId);
    List<Analysis> findByVideoIdIn(List<Long> videoIds);
    long countByVideoUserIdAndStatus(Long userId, Analysis.AnalysisStatus status);
}
