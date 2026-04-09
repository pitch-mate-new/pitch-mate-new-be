package com.example.pitchmateserver.rubric.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.rubric.dto.RubricResponse;
import com.example.pitchmateserver.rubric.service.RubricService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rubrics")
@RequiredArgsConstructor
public class RubricController {

    private final RubricService rubricService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RubricResponse>>> getAllRubrics() {
        return ResponseEntity.ok(ApiResponse.ok(rubricService.getAllRubrics()));
    }
}
