package com.example.pitchmateserver.feedback.repository;

import com.example.pitchmateserver.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByVideoIdOrderByStartTimeSecondsAsc(Long videoId);

    void deleteByVideoId(Long videoId);

    @Modifying
    @Query("UPDATE Feedback f SET f.author = null WHERE f.author.id = :authorId")
    void clearAuthor(@Param("authorId") Long authorId);
}
