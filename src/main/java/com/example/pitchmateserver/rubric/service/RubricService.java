package com.example.pitchmateserver.rubric.service;

import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.rubric.dto.RubricResponse;
import com.example.pitchmateserver.rubric.entity.Rubric;
import com.example.pitchmateserver.rubric.repository.RubricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RubricService {

    private final RubricRepository rubricRepository;

    public List<RubricResponse> getAllRubrics() {
        return rubricRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(RubricResponse::from)
                .toList();
    }

    public Rubric findRubric(Long rubricId) {
        return rubricRepository.findById(rubricId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RUBRIC_NOT_FOUND));
    }
}
