package com.example.pitchmateserver.rubric.repository;

import com.example.pitchmateserver.rubric.entity.Rubric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RubricRepository extends JpaRepository<Rubric, Long> {
    List<Rubric> findAllByOrderByDisplayOrderAsc();
}
