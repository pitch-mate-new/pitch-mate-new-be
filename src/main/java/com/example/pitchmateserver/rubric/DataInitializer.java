package com.example.pitchmateserver.rubric;

import com.example.pitchmateserver.evaluation.repository.EvaluationRepository;
import com.example.pitchmateserver.feedback.repository.FeedbackRepository;
import com.example.pitchmateserver.rubric.entity.Rubric;
import com.example.pitchmateserver.rubric.repository.RubricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RubricRepository rubricRepository;
    private final EvaluationRepository evaluationRepository;
    private final FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (rubricRepository.count() == 20) return;

        // 루브릭을 참조하는 데이터 먼저 삭제 (FK 제약 해제)
        // evaluation_scores는 Evaluation CASCADE ALL로 함께 삭제됨
        evaluationRepository.deleteAll();
        feedbackRepository.deleteAll();
        rubricRepository.deleteAll();

        rubricRepository.saveAll(List.of(
                // 스피치 (4개)
                Rubric.builder().title("발음 명확성").description("발음이 명확하고 정확한가").category("스피치").maxScore(10).displayOrder(1).build(),
                Rubric.builder().title("말 속도 적절성").description("적절한 속도로 말하는가").category("스피치").maxScore(10).displayOrder(2).build(),
                Rubric.builder().title("음성 변화").description("억양·강세·음량의 자연스러운 변화").category("스피치").maxScore(10).displayOrder(3).build(),
                Rubric.builder().title("발화 안정성").description("떨림 없이 안정적으로 말하는 정도").category("스피치").maxScore(10).displayOrder(4).build(),
                // 비언어 (4개)
                Rubric.builder().title("시선 처리").description("청중 또는 카메라와의 시선 접촉").category("비언어").maxScore(10).displayOrder(5).build(),
                Rubric.builder().title("제스처 활용").description("손동작 및 몸짓의 적절성").category("비언어").maxScore(10).displayOrder(6).build(),
                Rubric.builder().title("자세 안정성").description("올바른 자세와 흔들림 없는 몸의 안정성").category("비언어").maxScore(10).displayOrder(7).build(),
                Rubric.builder().title("표정 활용").description("발표 내용에 맞는 표정 관리").category("비언어").maxScore(10).displayOrder(8).build(),
                // 전달력·표현력 (12개)
                Rubric.builder().title("핵심 전달력").description("핵심 메시지 전달의 명확성").category("전달력·표현력").maxScore(10).displayOrder(9).build(),
                Rubric.builder().title("논리적 구성").description("내용의 논리적 흐름과 구조").category("전달력·표현력").maxScore(10).displayOrder(10).build(),
                Rubric.builder().title("내용 완성도").description("발표 내용의 충실함과 완성도").category("전달력·표현력").maxScore(10).displayOrder(11).build(),
                Rubric.builder().title("정보 정확도").description("전달되는 정보의 정확성").category("전달력·표현력").maxScore(10).displayOrder(12).build(),
                Rubric.builder().title("설득력").description("청중을 설득할 수 있는 표현력").category("전달력·표현력").maxScore(10).displayOrder(13).build(),
                Rubric.builder().title("필러워드 사용").description("'음', '어', '그' 등 불필요한 표현 빈도").category("전달력·표현력").maxScore(10).displayOrder(14).build(),
                Rubric.builder().title("시간 활용").description("주어진 시간 내 발표 완료 여부").category("전달력·표현력").maxScore(10).displayOrder(15).build(),
                Rubric.builder().title("발표 흐름").description("발표의 전반적인 흐름과 자연스러움").category("전달력·표현력").maxScore(10).displayOrder(16).build(),
                Rubric.builder().title("내용 연결성").description("단락 간 내용의 자연스러운 연결").category("전달력·표현력").maxScore(10).displayOrder(17).build(),
                Rubric.builder().title("자신감 표현").description("자신감 있는 태도와 표현").category("전달력·표현력").maxScore(10).displayOrder(18).build(),
                Rubric.builder().title("집중도 유지").description("청중의 집중을 유지하는 능력").category("전달력·표현력").maxScore(10).displayOrder(19).build(),
                Rubric.builder().title("전체 완성도").description("발표 전반의 종합적인 완성도").category("전달력·표현력").maxScore(10).displayOrder(20).build()
        ));
    }
}
