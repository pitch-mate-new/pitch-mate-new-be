package com.example.pitchmateserver.rubric.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rubrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Rubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "display_order")
    private Integer displayOrder;
}
