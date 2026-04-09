package com.example.pitchmateserver.feedback.repository;

import com.example.pitchmateserver.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByVideoIdOrderByStartTimeSecondsAsc(Long videoId);
}
