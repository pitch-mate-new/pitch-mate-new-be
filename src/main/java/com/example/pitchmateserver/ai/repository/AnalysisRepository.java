package com.example.pitchmateserver.ai.repository;

import com.example.pitchmateserver.ai.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    Optional<Analysis> findByVideoId(Long videoId);
    boolean existsByVideoId(Long videoId);
    List<Analysis> findByVideoIdIn(List<Long> videoIds);
    long countByVideoUserIdAndStatus(Long userId, Analysis.AnalysisStatus status);

    void deleteByVideoId(Long videoId);

    /**
     * FAILED 분석만 삭제한다. 조건부 DELETE라 동시에 재요청이 와도 한 건만 성공한다. 반환값: 삭제된 행 수(0 또는 1)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Analysis a WHERE a.id = :id AND a.status = com.example.pitchmateserver.ai.entity.Analysis.AnalysisStatus.FAILED")
    int deleteIfFailed(@Param("id") Long id);

    /**
     * staleBefore 이전부터 갱신이 없는 PENDING/IN_PROGRESS 작업을 FAILED로 전환한다 (QR-REL-04 고아 작업 정리)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Analysis a SET
                a.status = com.example.pitchmateserver.ai.entity.Analysis.AnalysisStatus.FAILED,
                a.errorMessage = :errorMessage,
                a.updatedAt = :now
            WHERE a.status IN (com.example.pitchmateserver.ai.entity.Analysis.AnalysisStatus.PENDING,
                               com.example.pitchmateserver.ai.entity.Analysis.AnalysisStatus.IN_PROGRESS)
              AND a.updatedAt < :staleBefore
            """)
    int failStale(@Param("staleBefore") LocalDateTime staleBefore,
                  @Param("now") LocalDateTime now,
                  @Param("errorMessage") String errorMessage);
}
