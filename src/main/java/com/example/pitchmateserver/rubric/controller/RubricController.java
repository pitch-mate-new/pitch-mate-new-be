package com.example.pitchmateserver.rubric.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.rubric.dto.RubricResponse;
import com.example.pitchmateserver.rubric.service.RubricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "루브릭", description = "평가 기준 항목 조회 API (인증 불필요)")
@RestController
@RequestMapping("/api/rubrics")
@RequiredArgsConstructor
public class RubricController {

    private final RubricService rubricService;

    @Operation(summary = "루브릭 항목 전체 조회", description = """
            AI 평가에 사용되는 10개 루브릭 항목을 반환합니다. 인증 없이 호출 가능합니다.

            **카테고리**
            - 스피치: 발음 정확성, 말하기 속도, 음성 변화, 시선 처리
            - 비언어: 제스처, 자세 및 표정
            - 전달력·표현력: 논리적 구성, 핵심전달력, 필러워드 빈도, 시간활용

            각 항목의 만점은 10점이며 총점은 100점입니다.
            """)
    @GetMapping
    public ResponseEntity<ApiResponse<List<RubricResponse>>> getAllRubrics() {
        return ResponseEntity.ok(ApiResponse.ok(rubricService.getAllRubrics()));
    }
}
