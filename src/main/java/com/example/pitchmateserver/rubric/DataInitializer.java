package com.example.pitchmateserver.rubric;

import com.example.pitchmateserver.rubric.entity.Rubric;
import com.example.pitchmateserver.rubric.repository.RubricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RubricRepository rubricRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (rubricRepository.count() == 10) return;

        // 기존 루브릭 전체 삭제 후 재등록 (항목 변경 시)
        rubricRepository.deleteAll();

        rubricRepository.saveAll(List.of(
                // 스피치
                Rubric.builder().title("발음 정확성").description("발음이 명확하고 정확한가").category("스피치").maxScore(10).displayOrder(1).build(),
                Rubric.builder().title("말하기 속도").description("적절한 속도로 말하는가").category("스피치").maxScore(10).displayOrder(2).build(),
                Rubric.builder().title("음성 변화").description("억양과 강약이 자연스럽고 효과적인가").category("스피치").maxScore(10).displayOrder(3).build(),
                Rubric.builder().title("시선 처리").description("청중(카메라)과 적절한 눈 맞춤을 유지하는가").category("스피치").maxScore(10).displayOrder(4).build(),
                // 비언어
                Rubric.builder().title("제스처").description("손짓과 몸짓이 내용 전달에 효과적으로 사용되는가").category("비언어").maxScore(10).displayOrder(5).build(),
                Rubric.builder().title("자세 및 표정").description("자세가 안정적이고 표정이 내용과 어울리는가").category("비언어").maxScore(10).displayOrder(6).build(),
                // 전달력·표현력
                Rubric.builder().title("논리적 구성").description("내용이 논리적인 흐름으로 구성되어 있는가").category("전달력·표현력").maxScore(10).displayOrder(7).build(),
                Rubric.builder().title("핵심전달력").description("핵심 메시지가 명확하게 전달되는가").category("전달력·표현력").maxScore(10).displayOrder(8).build(),
                Rubric.builder().title("필러워드 빈도").description("어, 음, 그 등 불필요한 필러워드 사용이 적은가").category("전달력·표현력").maxScore(10).displayOrder(9).build(),
                Rubric.builder().title("시간활용").description("주어진 시간을 효율적으로 활용했는가").category("전달력·표현력").maxScore(10).displayOrder(10).build()
        ));
    }
}
