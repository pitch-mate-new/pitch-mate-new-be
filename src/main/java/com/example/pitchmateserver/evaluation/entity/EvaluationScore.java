package com.example.pitchmateserver.evaluation.entity;

import com.example.pitchmateserver.rubric.entity.Rubric;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluation_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class EvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private Rubric rubric;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 500)
    private String comment;
}
