package com.example.pitchmateserver.rubric.dto;

import com.example.pitchmateserver.rubric.entity.Rubric;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RubricResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private Integer maxScore;
    private Integer displayOrder;

    public static RubricResponse from(Rubric rubric) {
        return RubricResponse.builder()
                .id(rubric.getId())
                .title(rubric.getTitle())
                .description(rubric.getDescription())
                .category(rubric.getCategory())
                .maxScore(rubric.getMaxScore())
                .displayOrder(rubric.getDisplayOrder())
                .build();
    }
}
