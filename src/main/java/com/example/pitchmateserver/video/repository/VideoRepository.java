package com.example.pitchmateserver.video.repository;

import com.example.pitchmateserver.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserIdOrderByCreatedAtDesc(Long userId);
}
