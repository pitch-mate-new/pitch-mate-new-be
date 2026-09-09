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

    @Operation(
            summary = "루브릭 항목 전체 조회",
            description = """
                    AI 평가에 사용되는 20개 루브릭 항목을 반환합니다. 인증 없이 호출 가능합니다.

                    **카테고리**
                    - 스피치 (4개): 발음 명확성, 말 속도 적절성, 음성 변화, 발화 안정성
                    - 비언어 (4개): 시선 처리, 제스처 활용, 자세 안정성, 표정 활용
                    - 전달력·표현력 (12개): 핵심 전달력, 논리적 구성, 내용 완성도, 정보 정확도, 설득력, 필러워드 사용, 시간 활용, 발표 흐름, 내용 연결성, 자신감 표현, 집중도 유지, 전체 완성도

                    각 항목의 만점은 10점(항목별 `maxScore`)이며, 평가 결과의 `totalScore`/`maxTotalScore`는 100점 만점으로 정규화되어 반환됩니다.

                    **에러 응답**
                    - 없음 (인증 불필요, 항상 10개 반환)
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<RubricResponse>>> getAllRubrics() {
        return ResponseEntity.ok(ApiResponse.ok(rubricService.getAllRubrics()));
    }
}
